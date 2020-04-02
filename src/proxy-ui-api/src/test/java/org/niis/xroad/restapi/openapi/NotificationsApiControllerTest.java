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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.niis.xroad.restapi.controller.NotificationsApiController;
import org.niis.xroad.restapi.domain.AlertData;
import org.niis.xroad.restapi.dto.AlertStatus;
import org.niis.xroad.restapi.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Test NotificationsApiController
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationsApiControllerTest {

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private NotificationsApiController notificationsApiController;

    @Test
    @WithMockUser
    public void checkAlerts() {
        AlertStatus alertStatus = new AlertStatus();
        alertStatus.setGlobalConfValid(true);
        alertStatus.setSoftTokenPinEntered(true);

        when(notificationService.getAlerts()).thenReturn(alertStatus);

        ResponseEntity<AlertData> response = notificationsApiController.checkAlerts();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        AlertData alertData = response.getBody();
        assertEquals(null, alertData.getBackupRestoreRunningSince());
        assertEquals(null, alertData.getCurrentTime());
        assertEquals(alertStatus.getGlobalConfValid(), alertData.getGlobalConfValid());
        assertEquals(alertStatus.getSoftTokenPinEntered(), alertData.getSoftTokenPinEntered());
    }

    @Test
    @WithMockUser
    public void checkAlertsBackupRestoreRunning() {
        OffsetDateTime date = OffsetDateTime.now(ZoneOffset.UTC);
        AlertStatus alertStatus = new AlertStatus();
        alertStatus.setBackupRestoreRunningSince(date);
        alertStatus.setCurrentTime(date);
        alertStatus.setGlobalConfValid(true);
        alertStatus.setSoftTokenPinEntered(true);

        when(notificationService.getAlerts()).thenReturn(alertStatus);

        ResponseEntity<AlertData> response = notificationsApiController.checkAlerts();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        AlertData alertData = response.getBody();
        assertEquals(alertStatus.getBackupRestoreRunningSince(), alertData.getBackupRestoreRunningSince());
        assertEquals(alertStatus.getCurrentTime(), alertData.getCurrentTime());
        assertEquals(alertStatus.getGlobalConfValid(), alertData.getGlobalConfValid());
        assertEquals(alertStatus.getSoftTokenPinEntered(), alertData.getSoftTokenPinEntered());
    }

    @Test
    @WithMockUser
    public void checkAlertsSoftTokenNotFound() {
        doThrow(new RuntimeException("")).when(notificationService).getAlerts();

        try {
            ResponseEntity<AlertData> response = notificationsApiController.checkAlerts();
            fail("should throw RuntimeException");
        } catch (RuntimeException expected) {
            // success
        }
    }
}