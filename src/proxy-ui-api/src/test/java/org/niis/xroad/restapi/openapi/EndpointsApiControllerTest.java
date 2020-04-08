/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.openapi;

import ee.ria.xroad.common.conf.serverconf.model.ClientType;
import ee.ria.xroad.common.conf.serverconf.model.EndpointType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.niis.xroad.restapi.facade.GlobalConfFacade;
import org.niis.xroad.restapi.openapi.model.Endpoint;
import org.niis.xroad.restapi.openapi.model.ServiceClient;
import org.niis.xroad.restapi.openapi.model.ServiceClientType;
import org.niis.xroad.restapi.openapi.model.ServiceClients;
import org.niis.xroad.restapi.service.ClientService;
import org.niis.xroad.restapi.service.GlobalConfService;
import org.niis.xroad.restapi.util.PersistenceUtils;
import org.niis.xroad.restapi.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.niis.xroad.restapi.util.TestUtils.getClientId;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
public class EndpointsApiControllerTest {

    @Autowired
    private EndpointsApiController endpointsApiController;

    @Autowired
    private ClientService clientService;

    @Autowired
    private PersistenceUtils persistenceUtils;

    @MockBean
    private GlobalConfFacade globalConfFacade;

    @MockBean
    private GlobalConfService globalConfService;

    private static final String NO_SUCH_ENDPOINT_ID = "1294379018";

    @Test
    @WithMockUser(authorities = {"VIEW_ENDPOINT"})
    public void getEndpoint() {
        Endpoint endpoint = endpointsApiController.getEndpoint("12").getBody();
        assertTrue(endpoint.getId().equals("12"));
        assertTrue(endpoint.getMethod().equals(Endpoint.MethodEnum.PUT));
        assertTrue(endpoint.getPath().equals("/foo"));
    }

    @Test(expected = ResourceNotFoundException.class)
    @WithMockUser(authorities = {"DELETE_ENDPOINT"})
    public void deleteEndpointNotExist() {
        endpointsApiController.deleteEndpoint(NO_SUCH_ENDPOINT_ID);
    }

    @Test(expected = BadRequestException.class)
    @WithMockUser(authorities = {"DELETE_ENDPOINT"})
    public void deleteGeneratedEndpoint() {
        endpointsApiController.deleteEndpoint("10");
    }

    @Test
    @WithMockUser(authorities = {"DELETE_ENDPOINT"})
    public void deleteEndpoint() {
        ClientType client = clientService.getLocalClient(getClientId("FI", "GOV", "M2", "SS6"));
        int aclCount = client.getAcl().size();
        endpointsApiController.deleteEndpoint("11");
        assertTrue(client.getEndpoint().stream().noneMatch(ep -> ep.getId().equals(11L)));
        assertTrue(client.getAcl().size() < aclCount);
    }

    @Test(expected = BadRequestException.class)
    @WithMockUser(authorities = {"EDIT_OPENAPI3_ENDPOINT"})
    public void updateGeneratedEndpoint() {
        Endpoint endpointUpdate = new Endpoint();
        endpointUpdate.setId("10");
        endpointUpdate.setServiceCode("TestServiceCode");
        endpointUpdate.setMethod(Endpoint.MethodEnum.STAR);
        endpointUpdate.setPath("/test");
        endpointUpdate.setGenerated(false);
        endpointsApiController.updateEndpoint("10", endpointUpdate);
    }

    @Test
    @WithMockUser(authorities = {"EDIT_OPENAPI3_ENDPOINT"})
    public void updateEndpoint() {
        Endpoint endpointUpdate = new Endpoint();
        endpointUpdate.setId("12");
        endpointUpdate.setServiceCode("TestServiceCode");
        endpointUpdate.setMethod(Endpoint.MethodEnum.STAR);
        endpointUpdate.setPath("/test");
        endpointUpdate.setGenerated(false);
        endpointsApiController.updateEndpoint("12", endpointUpdate);

        ClientType client = clientService.getLocalClient(getClientId("FI", "GOV", "M2", "SS6"));
        EndpointType endpointType = client.getEndpoint().stream().filter(ep -> ep.getId().equals(12L))
                .findFirst().get();

        assertTrue(endpointType.getServiceCode().equals("TestServiceCode"));
        assertTrue(endpointType.getMethod().equals("*"));
        assertTrue(endpointType.getPath().equals("/test"));
    }

    @Test(expected = ResourceNotFoundException.class)
    @WithMockUser(authorities = {"VIEW_ENDPOINT_ACL"})
    public void getInexistingEndpointAccessRights() {
        endpointsApiController.getEndpointServiceClients("NON_EXISTING_ENDPOINT_ID");
    }

    @Test
    @WithMockUser(authorities = {"VIEW_ENDPOINT_ACL"})
    public void getEndpointAccesRights() {
        List<ServiceClient> serviceClients = endpointsApiController.getEndpointServiceClients("6").getBody();
        assertTrue(serviceClients.size() == 2);
        assertTrue(serviceClients.stream()
                .anyMatch(sc -> sc.getId().equals(TestUtils.CLIENT_ID_SS6)));
        assertTrue(serviceClients.stream()
                .anyMatch(sc -> sc.getId().equals("2")));
    }

    @Test
    @WithMockUser(authorities = {"EDIT_ENDPOINT_ACL", "VIEW_ENDPOINT_ACL"})
    public void removeExistingEndpointAccessRights() {
        List<ServiceClient> serviceClients = endpointsApiController.getEndpointServiceClients("6").getBody();
        assertTrue(serviceClients.size() == 2);
        ServiceClients subjects = new ServiceClients()
                .addItemsItem(new ServiceClient().id(TestUtils.CLIENT_ID_SS6).serviceClientType(
                        ServiceClientType.SUBSYSTEM));
        endpointsApiController.deleteEndpointServiceClients("6", subjects);
        persistenceUtils.flush();
        serviceClients = endpointsApiController.getEndpointServiceClients("6").getBody();
        assertTrue(serviceClients.size() == 1);
        assertTrue(serviceClients.stream().findFirst().get().getId().equals("2"));
    }

    @Test(expected = ResourceNotFoundException.class)
    @WithMockUser(authorities = {"EDIT_ENDPOINT_ACL", "VIEW_ENDPOINT_ACL"})
    public void removeInexistingEndpointAccessRights() {
        List<ServiceClient> serviceClients = endpointsApiController.getEndpointServiceClients("6").getBody();
        assertTrue(serviceClients.size() == 2);
        ServiceClients subjects = new ServiceClients()
                .addItemsItem(new ServiceClient().id(TestUtils.CLIENT_ID_SS1).serviceClientType(
                        ServiceClientType.SUBSYSTEM));
        endpointsApiController.deleteEndpointServiceClients("6", subjects);
    }

    @Test(expected = ConflictException.class)
    @WithMockUser(authorities = {"EDIT_ENDPOINT_ACL"})
    public void addExistingEndpointAccessRights() {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(true);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(true);

        ServiceClients subjects = new ServiceClients()
                .addItemsItem(new ServiceClient().id(TestUtils.CLIENT_ID_SS6).serviceClientType(
                        ServiceClientType.SUBSYSTEM));
        endpointsApiController.addEndpointServiceClients("9", subjects);
    }

    @Test
    @WithMockUser(authorities = {"VIEW_ENDPOINT_ACL", "EDIT_ENDPOINT_ACL"})
    public void addEndpointAccessRights() {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(true);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(true);

        // add access rights for a subsystem and global group to endpoint
        List<ServiceClient> serviceClients = endpointsApiController.getEndpointServiceClients("9").getBody();
        assertTrue(serviceClients.size() == 1);
        ServiceClients subjects = new ServiceClients()
                .addItemsItem(new ServiceClient().id(TestUtils.CLIENT_ID_SS5).serviceClientType(
                        ServiceClientType.SUBSYSTEM))
                .addItemsItem(new ServiceClient().id(TestUtils.DB_GLOBALGROUP_ID).serviceClientType(
                        ServiceClientType.GLOBALGROUP));
        endpointsApiController.addEndpointServiceClients("9", subjects).getBody();
        persistenceUtils.flush();
        serviceClients = endpointsApiController.getEndpointServiceClients("9").getBody();

        assertTrue(serviceClients.size() == 3);
        assertTrue(serviceClients.stream().anyMatch(sc -> sc.getId().equals(TestUtils.CLIENT_ID_SS5)));
        assertTrue(serviceClients.stream().anyMatch(sc -> sc.getId().equals(TestUtils.DB_GLOBALGROUP_ID)));

        // add access rights for a local group to endpoint
        List<ServiceClient> localGroupTestServiceClients = endpointsApiController
                .getEndpointServiceClients("3").getBody();
        assertTrue(localGroupTestServiceClients.size() == 0);
        ServiceClients localGroupSubjects = new ServiceClients()
                .addItemsItem(new ServiceClient().id(TestUtils.DB_LOCAL_GROUP_ID_1).serviceClientType(
                        ServiceClientType.LOCALGROUP));
        endpointsApiController.addEndpointServiceClients("3", localGroupSubjects).getBody();
        persistenceUtils.flush();
        localGroupTestServiceClients = endpointsApiController.getEndpointServiceClients("3").getBody();

        assertTrue(localGroupTestServiceClients.size() == 1);
        assertTrue(localGroupTestServiceClients.stream().anyMatch(sc -> sc.getId()
                .equals(TestUtils.DB_LOCAL_GROUP_ID_1)));
    }


}
