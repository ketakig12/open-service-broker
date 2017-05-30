package com.swisscom.cloud.sb.broker.provisioning.job

import com.swisscom.cloud.sb.broker.async.job.AbstractLastOperationJob
import com.swisscom.cloud.sb.broker.provisioning.lastoperation.LastOperationJobContext
import com.swisscom.cloud.sb.broker.model.LastOperation
import com.swisscom.cloud.sb.broker.model.ProvisionRequest
import com.swisscom.cloud.sb.broker.model.repository.ProvisionRequestRepository
import com.swisscom.cloud.sb.broker.model.repository.ServiceInstanceRepository
import com.swisscom.cloud.sb.broker.provisioning.ProvisionResponse
import com.swisscom.cloud.sb.broker.services.common.ServiceProviderLookup
import com.swisscom.cloud.sb.broker.provisioning.async.AsyncServiceProvisioner
import com.swisscom.cloud.sb.broker.provisioning.async.AsyncOperationResult
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@CompileStatic
@Component
@Slf4j
//When renaming, the existing jobs in Quartz DB should be renamed accordingly!!!
class ServiceProvisioningJob extends AbstractLastOperationJob {
    @Autowired
    private ServiceProviderLookup serviceProviderLookup
    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository
    @Autowired
    private ProvisionRequestRepository provisionRequestRepository

    protected LastOperationJobContext enrichContext(LastOperationJobContext context) {
        String serviceInstanceGuid = context.lastOperation.guid
        ProvisionRequest provisionRequest = provisionRequestRepository.findByServiceInstanceGuid(serviceInstanceGuid)
        context.provisionRequest = provisionRequest
        context.plan = provisionRequest.plan
        context.serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
        return context
    }

    @Override
    protected AsyncOperationResult handleJob(LastOperationJobContext context) {
        log.info("About to request service provision, ${context.lastOperation.toString()}")
        AsyncOperationResult provisionResult = findServiceProvisioner(context).requestProvision(context)
        provisioningPersistenceService.createServiceInstanceOrUpdateDetails(context.provisionRequest, new ProvisionResponse(details: provisionResult.details, isAsync: true))
        if (provisionResult.status == LastOperation.Status.SUCCESS) {
            provisioningPersistenceService.updateServiceInstanceCompletion(context.serviceInstance, true)
        }
        return provisionResult
    }

    private AsyncServiceProvisioner findServiceProvisioner(LastOperationJobContext context) {
        AsyncServiceProvisioner serviceProvisioner = ((AsyncServiceProvisioner) serviceProviderLookup.findServiceProvider(context.plan))
        return serviceProvisioner
    }
}

