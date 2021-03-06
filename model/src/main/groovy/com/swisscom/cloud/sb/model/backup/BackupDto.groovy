package com.swisscom.cloud.sb.model.backup

import groovy.transform.CompileStatic

@CompileStatic
class BackupDto implements Serializable {
    String id
    String service_instance_id
    Date created_at
    Date updated_at
    BackupStatus status
    Collection<RestoreDto> restores
}
