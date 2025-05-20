/*
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 *  © 2022 Wowza Media Systems, LLC. All rights reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of Wowza Media Systems, LLC.
 * The intellectual and technical concepts contained herein are proprietary to Wowza Media Systems, LLC
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 * or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from Wowza Media Systems, LLC. Access to the source code
 * contained herein is hereby forbidden to anyone except current Wowza Media Systems, LLC employees, managers
 * or contractors who have executed Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this
 * source code, which includes information that is confidential and/or proprietary, and is a trade secret, of
 * Wowza Media Systems, LLC. ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF WOWZA MEDIA SYSTEMS, LLC IS
 * STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION
 * OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR
 * DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */

package com.wowza.wms.plugin.captions.azure;

import org.junit.jupiter.api.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ByteArrayAudioStreamTest
{

    private ByteArrayAudioStream stream;

    @BeforeEach
    void setUp()
    {
        stream = new ByteArrayAudioStream();
        for (int i = 0; i < 10; i++)
        {
            stream.write(new byte[1024]);
        }
    }

    @Test
    void testReadReturnsFullByteArray()
    {
        byte[] buffer = new byte[4096];
        int read = stream.read(buffer);
        assertEquals(4096, read);
    }

    @Test
    void testReadReturnsPartialWrittenByteArray()
    {
        byte[] buffer = new byte[10000];
        int len = stream.read(buffer);
        assertEquals(10000, len);
    }

    @Test
    void ReadReturnsPartialByteArrayWhenQueueIsDrained()
    {
        byte[] buffer = new byte[16000];
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> task = () -> stream.read(buffer);
        Future<Integer> future = executor.submit(task);
        Integer len = null;
        try
        {
            len = future.get(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            // ignored
        }
        assertEquals(10240, len);
    }

    @Test
    void ReadBlocksIfQueueIsEmpty()
    {
        byte[] buffer = new byte[10240];
        stream.read(buffer);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> task = () -> stream.read(buffer);
        Future<Integer> future = executor.submit(task);
        assertThrows(TimeoutException.class, () -> future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testReadReturnsZeroIfStreamIsClosed()
    {
        stream.close();
        byte[] buffer = new byte[1024];
        int len = stream.read(buffer);
        assertEquals(0, len);
    }

    @Test
    void testReadWaitsForWrite() throws ExecutionException, InterruptedException, TimeoutException
    {
        byte[] buffer = new byte[10240];
        stream.read(buffer);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> task = () -> stream.read(buffer);
        Future<Integer> future = executor.submit(task);
        System.out.println("availableBeforeWrite = " + stream.available());
        stream.write(new byte[1024]);
        System.out.println("availableAfterWrite = " + stream.available());
        Integer len = future.get(1, TimeUnit.SECONDS);
        assertEquals(1024, len);
    }
}