/*
 *  The MIT License
 *  Copyright (c) 2018 Estonian Information System Authority (RIA),
 *  Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 *  Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.niis.xroad.restapi.service;

import ee.ria.xroad.common.conf.globalconf.GlobalGroupInfo;
import ee.ria.xroad.common.conf.globalconf.MemberInfo;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.GlobalGroupId;
import ee.ria.xroad.common.identifier.LocalGroupId;
import ee.ria.xroad.common.identifier.XRoadId;
import ee.ria.xroad.common.identifier.XRoadObjectType;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.niis.xroad.restapi.dto.ServiceClientDto;
import org.niis.xroad.restapi.facade.GlobalConfFacade;
import org.niis.xroad.restapi.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * test Service service
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase
@Slf4j
@Transactional
@WithMockUser
public class AccessRightServiceTest {

    private List<MemberInfo> memberInfos = new ArrayList<>(Arrays.asList(
            TestUtils.getMemberInfo(TestUtils.INSTANCE_FI, TestUtils.MEMBER_CLASS_GOV, TestUtils.MEMBER_CODE_M1, null),
            TestUtils.getMemberInfo(TestUtils.INSTANCE_EE, TestUtils.MEMBER_CLASS_GOV, TestUtils.MEMBER_CODE_M1,
                    TestUtils.SUBSYSTEM1),
            TestUtils.getMemberInfo(TestUtils.INSTANCE_FI, TestUtils.MEMBER_CLASS_GOV, TestUtils.MEMBER_CODE_M1,
                    TestUtils.SUBSYSTEM1)));
    private List<GlobalGroupInfo> globalGroupInfos = new ArrayList<>(Arrays.asList(
            TestUtils.getGlobalGroupInfo(TestUtils.INSTANCE_FI, TestUtils.DB_GLOBALGROUP_CODE),
            TestUtils.getGlobalGroupInfo(TestUtils.INSTANCE_FI, TestUtils.GLOBALGROUP_CODE1),
            TestUtils.getGlobalGroupInfo(TestUtils.INSTANCE_EE, TestUtils.DB_GLOBALGROUP_CODE)));
    private List<String> instanceIdentifiers = new ArrayList<>(Arrays.asList(
            TestUtils.INSTANCE_FI,
            TestUtils.INSTANCE_EE));

    @Autowired
    AccessRightService accessRightService;

    @MockBean
    GlobalConfFacade globalConfFacade;

    @MockBean
    GlobalConfService globalConfService;

    @Before
    public void setup() {
        when(globalConfFacade.getMembers()).thenReturn(memberInfos);
        when(globalConfFacade.getInstanceIdentifier()).thenReturn(TestUtils.INSTANCE_FI);
        when(globalConfFacade.getInstanceIdentifiers()).thenReturn(instanceIdentifiers);
        when(globalConfFacade.getGlobalGroups(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            if (args.length == 0) {
                return globalGroupInfos;
            }
            List<String> foundInstanceIdentifiers = new ArrayList<>(Arrays.asList(
                    Arrays.copyOf(args, args.length, String[].class)));
            return globalGroupInfos.stream()
                    .filter(globalGroupInfo -> foundInstanceIdentifiers
                            .contains(globalGroupInfo.getId().getXRoadInstance()))
                    .collect(Collectors.toList());
        });
    }

    @Test
    public void findAllAccessRightHolders() throws Throwable {
        List<ServiceClientDto> dtos = accessRightService.findAccessRightHolderCandidates(TestUtils.getM1Ss1ClientId(),
                null, null, null, null, null, null);
        assertEquals(7, dtos.size());
    }

    @Test
    public void findAccessRightHoldersByMemberOrGroupCode() throws Throwable {
        List<ServiceClientDto> dtos = accessRightService.findAccessRightHolderCandidates(TestUtils.getM1Ss1ClientId(),
                null, null, null, null, "1", null);
        assertEquals(4, dtos.size());
    }

    @Test
    public void findAccessRightHoldersByMemberOrGroupCodeNoResults() throws Throwable {
        List<ServiceClientDto> dtos = accessRightService.findAccessRightHolderCandidates(TestUtils.getM1Ss1ClientId(),
                null, null, null, null, "öäöäöäöäöäöä", null);
        assertEquals(0, dtos.size());
    }

    @Test
    public void findAccessRightHoldersByInstance() throws Throwable {
        List<ServiceClientDto> dtos = accessRightService.findAccessRightHolderCandidates(TestUtils.getM1Ss1ClientId(),
                null, null, TestUtils.INSTANCE_EE, null, null, null);
        assertEquals(4, dtos.size());
    }

    @Test
    public void findAccessRightHoldersByInstanceAndSubSystem() throws Throwable {
        List<ServiceClientDto> dtos = accessRightService.findAccessRightHolderCandidates(TestUtils.getM1Ss1ClientId(),
                null, null, TestUtils.INSTANCE_FI, null, null, TestUtils.SUBSYSTEM1);
        assertEquals(1, dtos.size());
    }

    @Test
    public void addAccessRights() throws Throwable {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(true);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(true);
        ClientId clientId = TestUtils.getM1Ss1ClientId();
        Set<XRoadId> subjectIds = new HashSet<>();
        subjectIds.add(ClientId.create(TestUtils.INSTANCE_FI, TestUtils.MEMBER_CLASS_GOV, TestUtils.MEMBER_CODE_M1,
                TestUtils.SUBSYSTEM2));
        subjectIds.add(GlobalGroupId.create(TestUtils.INSTANCE_FI, TestUtils.DB_GLOBALGROUP_CODE));
        Set<Long> localGroupIds = new HashSet<>();
        localGroupIds.add(2L);
        List<ServiceClientDto> dtos = accessRightService.addSoapServiceAccessRights(clientId,
                TestUtils.SERVICE_CALCULATE_PRIME, subjectIds, localGroupIds);
        assertEquals(3, dtos.size());
    }

    @Test
    public void addAccessRightsForNonLocalSubsystem() throws Throwable {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(true);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(true);
        ClientId clientId = TestUtils.getM1Ss1ClientId();
        Set<XRoadId> subjectIds = new HashSet<>();
        ClientId ss3 = ClientId.create(TestUtils.INSTANCE_FI, TestUtils.MEMBER_CLASS_GOV, TestUtils.MEMBER_CODE_M1,
                TestUtils.SUBSYSTEM3);
        Long ss3Pk = (Long) ReflectionTestUtils.getField(ss3, "id");
        assertNull(ss3Pk); // SS3 does not exists (no primary key) but it will be created
        subjectIds.add(ss3);
        subjectIds.add(GlobalGroupId.create(TestUtils.INSTANCE_FI, TestUtils.DB_GLOBALGROUP_CODE));
        Set<Long> localGroupIds = new HashSet<>();
        localGroupIds.add(2L);
        List<ServiceClientDto> dtos = accessRightService.addSoapServiceAccessRights(clientId,
                TestUtils.SERVICE_CALCULATE_PRIME, subjectIds, localGroupIds);
        assertEquals(3, dtos.size());
        ServiceClientDto persistedSs3 = dtos.stream()
                .filter(accessRightHolderDto -> accessRightHolderDto.getSubjectId().equals(ss3))
                .findFirst().get();
        ClientId ss3PersistedSubjectId = (ClientId) persistedSs3.getSubjectId();
        Long ss3PersistedPk = (Long) ReflectionTestUtils.getField(ss3PersistedSubjectId, "id");
        assertNotNull(ss3PersistedPk); // SS3 has a primary key
    }

    @Test(expected = IdentifierNotFoundException.class)
    public void addAccessRightsForNonExistingClient() throws Throwable {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(false);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(false);
        ClientId clientId = TestUtils.getM1Ss1ClientId();
        Set<XRoadId> subjectIds = new HashSet<>();
        subjectIds.add(ClientId.create(TestUtils.INSTANCE_FI, TestUtils.MEMBER_CLASS_GOV, TestUtils.MEMBER_CODE_M1,
                "nope"));
        Set<Long> localGroupIds = new HashSet<>();
        localGroupIds.add(2L);
        accessRightService.addSoapServiceAccessRights(clientId, TestUtils.SERVICE_CALCULATE_PRIME, subjectIds,
                localGroupIds);
    }

    @Test(expected = IdentifierNotFoundException.class)
    public void addDuplicateAccessRights() throws Throwable {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(false);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(false);
        ClientId clientId = TestUtils.getM1Ss1ClientId();
        Set<XRoadId> subjectIds = new HashSet<>();
        subjectIds.add(ClientId.create(TestUtils.INSTANCE_FI, TestUtils.MEMBER_CLASS_GOV, TestUtils.MEMBER_CODE_M1,
                "nope"));
        Set<Long> localGroupIds = new HashSet<>();
        localGroupIds.add(1L);
        accessRightService.addSoapServiceAccessRights(clientId, TestUtils.SERVICE_CALCULATE_PRIME, subjectIds,
                localGroupIds);
    }

    @Test
    public void addAccessRightsToLocalGroup() throws Throwable {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(true);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(true);
        ClientId clientId = TestUtils.getM1Ss1ClientId();
        Set<Long> localGroupIds = new HashSet<>();
        localGroupIds.add(1L); // this is a LocalGroup with groupCode 'group1' in data.sql
        List<ServiceClientDto> aclHolders = accessRightService.addSoapServiceAccessRights(clientId,
                TestUtils.SERVICE_CALCULATE_PRIME, null, localGroupIds);
        assertEquals(LocalGroupId.create(TestUtils.DB_LOCAL_GROUP_CODE), aclHolders.get(0).getSubjectId());
    }

    @Test(expected = LocalGroupNotFoundException.class)
    public void addAccessRightsToOtherClientsLocalGroup() throws Throwable {
        when(globalConfService.clientIdentifiersExist(any())).thenReturn(true);
        when(globalConfService.globalGroupIdentifiersExist(any())).thenReturn(true);
        ClientId clientId = TestUtils.getM1Ss2ClientId();
        Set<Long> localGroupIds = new HashSet<>();
        localGroupIds.add(1L);
        accessRightService.addSoapServiceAccessRights(clientId, TestUtils.SERVICE_BMI_OLD, null,
                localGroupIds);
    }

    @Test(expected = ClientNotFoundException.class)
    public void getClientServiceClientsFromUnexistingClient() throws Exception {
        accessRightService.getAccessRightHoldersByClient(ClientId.create("NO", "SUCH", "CLIENT"));
    }

    @Test
    public void getClientServiceClients() throws Exception {
        ClientId clientId1 = ClientId.create("FI", "GOV", "M2", "SS6");
        List<ServiceClientDto> serviceClients1 = accessRightService.getAccessRightHoldersByClient(clientId1);
        assertTrue(serviceClients1.size() == 1);

        ServiceClientDto arh1 = serviceClients1.get(0);
        assertTrue(arh1.getSubjectId().getObjectType().equals(XRoadObjectType.SUBSYSTEM));
        assertNull(arh1.getLocalGroupCode());
        assertNull(arh1.getLocalGroupDescription());
        assertNull(arh1.getLocalGroupId());
        assertTrue(arh1.getSubjectId().getXRoadInstance().equals("FI"));

        ClientId clientId2 = ClientId.create("FI", "GOV", "M1");
        assertTrue(accessRightService.getAccessRightHoldersByClient(clientId2).isEmpty());

        ClientId clientId3 = ClientId.create("FI", "GOV", "M1", "SS1");
        List<ServiceClientDto> serviceClients3 = accessRightService.getAccessRightHoldersByClient(clientId3);
        assertTrue(serviceClients3.size() == 3);
        assertTrue(serviceClients3.stream().anyMatch(arh -> arh.getSubjectId()
                .getObjectType().equals(XRoadObjectType.GLOBALGROUP)));
        assertTrue(serviceClients3.stream().anyMatch(arh -> arh.getSubjectId()
                .getObjectType().equals(XRoadObjectType.LOCALGROUP)));
        assertTrue(serviceClients3.stream().anyMatch(arh -> arh.getSubjectId()
                .getObjectType().equals(XRoadObjectType.SUBSYSTEM)
                && arh.getSubjectId().getXRoadInstance().equals("FI")));

    }
}
