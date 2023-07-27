# Vault Secret Provider

## Hands-on to use it in gravitee.yaml

This README show you how to configure gravitee and Vault with a basic use case.

## Setup for self-testing (in mem storage)

### Install vault:

`brew tap hashicorp/tap`

`brew install hashicorp/tap/vault`

### Start Vault
`vault server -dev`

*Copy the root token from thr logs*
```
The unseal key and root token are displayed below in case you want to
seal/unseal the Vault or re-authenticate.

Unseal Key: THE_UN_SEAL_KEY_fds587ds58e
Root Token: s.THE_ROOT_TOKEN_387f3d387KJHFE

Development mode should NOT be used in production installations!
```

open another terminal
```BASH
export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_DEV_ROOT_TOKEN=s.THE_ROOT_TOKEN_387f3d387KJHFE
```
then do to get logged as root
```BASH
vault login (root token prompted), type it again
```

### Configure a secret

For the sake of simplicity, we are hiding the Mongo DB name, but it'd the same for its password.

Create a secret
```BASH
vault kv put -mount=secret gravitee/mongo dbname=gravitee
```

Test
```BASH
vault kv get -mount=secret -field=dbname gravitee/mongo
```

```
======= Secret Path =======
secret/data/gravitee/mongo

======= Metadata =======
Key                Value
---                -----
created_time       2023-07-25T14:15:52.036484Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            4

===== Data =====
Key        Value
---        -----
dbname     gravitee
```

Make our secret read-only with a policy.

We could create several and bind some of them when setting up auth or create a token.

```BASH
vault policy write gravitee-read - << EOF

path "secret/data/gravitee/*" {
  capabilities = ["read"]
}

# if we had other secret to make available
# to the config, we could add them here

EOF
```

## Configure Gravitee to use vault

This the full config. Later the bear minimum will be shown only is useful

Add the following to gravitee.yaml (if not set yet)

```YAML
secrets:
  vault:
    enabled: false
    host: 127.0.0.1
    port: 8200
    # nameSpace: default
    # kvEngine: V2                          # defaults to v2 can be "v1", no mixing supported
    # readTimeoutSec: 2
    # connectTimeoutSec: 3
    ssl:
      enabled: false                        # not for production
        # format: "pemfile"                   # one of "pem","pemfile" "keystore", "truststore"
        # pem:                                # pem in base64 with headers value
        # file: /opt/gravitee/vault.pem       # also used for keystore and truststore files
        # password:                           # for key store, otherwise ignored
        # mTLS:
        # enabled: false
        # format: pemfile                    # can also be 'pem' to inline the cert (same as bove)
        # cert:                              # filename or inline cert
      # key:                               # filename or inline private key
    auth:
      method: token # one of "token", "github", "userpass", "approle"
      ### github config
      config:
        token:
        ### github config
        # token: 
        # path: <non standard github path>
        ### userpass config
        # username:
        # password:
        # path: <non standard github path>
        ### approle
        # roleId:
        # secretId:
    # RECOMMENDED but works without
    # for both watch and read
    retry:
      attempts: 2          # set '0' to disable
      intervalMs: 1000
    # if false an error will be displayed at load time it will start up anyway
    watch:
      enabled: true
      pollIntervalSec: 30
```

### Authenticate with a token

Create a policy-specific token to use in gravitee, this wil allow to restrict to reading only our secret.

```BASH
export VAULT_GRAVITEE_TOKEN=$(vault token create -field token -policy=gravitee-read)
```

Introspect
```BASH
vault token lookup $VAULT_GRAVITEE_TOKEN
```

Test read
```BASH
VAULT_TOKEN=$VAULT_GRAVITEE_TOKEN vault kv get -mount=secret gravitee/mongo
```

Test write (should get a 403)
```BASH
VAULT_TOKEN=$VAULT_GRAVITEE_TOKEN vault kv put -mount=secret gravitee/mongo dbname=gravitee
```

you can set `gravitee.yml` as follow (extract)
```YAML
secrets:
    vault:
        auth:
            method: token
            config:
                token: <value of $VAULT_GRAVITEE_TOKEN>
...
ds:
    mongodb:  
        dbname: secret://vault/secret/gravitee/mongo/dbname
```

### Authenticate with an AppRole

Weapon of choice for applications, we use a role (it's id) and there is multiple way to create a secret_id, you can trust third party to generate it and wrap it for a use-once only by your application. CI can generate it, and make it available to Gravitee. Here we don't support this use case yet, we just set it in the config. No need for a token, the plugin we ask vault to create one.

Enable it
```BASH
vault auth enable approle
```

Create an app role specific for our policy
```BASH
# Some short usage (e.g for integration testing purposes)
# can log-in for 30 mins (default, can be changed at creation time)
# once logged in can be used for 10 mins
# can call Vault API 50 times within 10 mins
vault write auth/approle/role/gravitee-conf \
    secret_id_ttl=30m \  
    token_ttl=10m \     
    token_num_uses=50 \ 
    secret_id_num_uses=5 \ # can login-in 5 times
    token_policies=gravitee-read

# long lived version for a prod install
vault write auth/approle/role/gravitee-conf \ 
    token_ttl=10m \
    token_num_uses=0 \
    secret_id_num_uses=0 \       
    token_policies=gravitee-read
    
export ROLE_ID="$(vault read -field=role_id auth/approle/role/gravitee-conf/role-id)"

export SECRET_ID="$(vault write -f -field=secret_id auth/approle/role/gravitee-conf/secret-id)"
```

Test with CLI
Create a new token (this what we are doing in the plugin)

```BASH
export VAULT_APP_ROLE=$(vault write -field=token auth/approle/login role_id="$ROLE_ID" secret_id="$SECRET_ID")
```

You can copy the token and do the following
```BASH
VAULT_TOKEN=$VAULT_APP_ROLE vault kv get -mount=secret gravitee/mongo
```

Edit `gravitee.yml` as follows (extract)
```YAML
secrets:
    vault:
        auth:
            method: approle
            config:
                roleId: <value of $ROLE_ID>
                secretId: <value of $SECRET_ID>
...
ds:
    mongodb:  
        dbname: secret://vault/secret/gravitee/mongo/dbname
                
```

### Authenticate with User/Pass

For development basic uses cases.

Enable it
```BASH
vault auth enable userpass
````

Create a password and attach policies to it (here with a short renew period but infinite logins)
```BASH
vault write auth/userpass/users/admin \
    password=changeme \
    policies=gravitee-read \
    token_ttl=1m \
    token_max_ttl=2m
```

Update `gravitee.yml`

```YAML
secrets:
    vault:
        auth:
            method: userpass
            config:
                username: admin
                password: changeme
...
ds:
    mongodb:  
        dbname: secret://vault/secret/gravitee/mongo/dbname

```

### Authenticate with GitHub

Could be useful with a shared Vault instance for developers.

Enable it
```BASH
vault auth enable github
```

Configure it to get data from `gravitee-io` if you are part of it ;-) 

```BASH
vault write auth/github/config organization=gravitee-io
```

Map our policy

To a team
```BASH
vault write auth/github/map/teams/<team> value=gravitee-read
```

Or to a specific user
```BASH
vault write auth/github/map/users/<github user> value=gravitee-read
```

Create a personal GitHub token with role `org:read` at least

Your Profile => Settings => Developer Settings => Personal Token => Classic => Choose `org:read`

Adapt the gravitee.yml

```YAML
secrets:
    vault:
        auth:
            method: github
            config:
                token: <your personal github token here>
                
ds:
    mongodb:  
        dbname: secret://vault/secret/gravitee/mongo/dbname
```

