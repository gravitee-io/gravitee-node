package io.gravitee.node.api.license.model;

import java.util.Date;
import lombok.Data;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class License {

    private String id;
    private String referenceType;
    private String referenceId;
    private String content;
    private Date createdAt;
    private Date updatedAt;
}
