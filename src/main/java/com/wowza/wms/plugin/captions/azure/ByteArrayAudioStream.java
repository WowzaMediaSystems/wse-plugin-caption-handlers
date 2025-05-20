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

import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ByteArrayAudioStream extends PullAudioInputStreamCallback implements AutoCloseable
{
    private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger available = new AtomicInteger(0);
    private int currentPos = 0;
    private byte[] currentFrame;


    @Override
    public int read(byte[] dataBuffer)
    {
        if (closed.get())
            return 0;

        int ret = 0;
        try
        {
            ret = fillDataBuffer(dataBuffer);
        }
        catch (InterruptedException e)
        {
            closed.set(true);
        }
        return ret;
    }

    private int fillDataBuffer(byte[] dataBuffer) throws InterruptedException
    {
        int pos = 0;
        while(pos < dataBuffer.length)
        {
            if (pos > 0 && queue.isEmpty())
                break;
            int remaining = dataBuffer.length - pos;
            if (currentFrame == null)
                currentFrame = queue.take();
            int len = Math.min(currentFrame.length - currentPos, remaining);
            System.arraycopy(currentFrame, currentPos, dataBuffer, pos, len);
            currentPos += len;
            if (currentPos == currentFrame.length)
            {
                currentPos = 0;
                currentFrame = null;
            }
            pos += len;
            available.getAndAdd(-len);
        }
        return pos;
    }

    @Override
    public void close()
    {
        closed.set(true);
        available.set(0);
        queue.clear();
    }

    public int available()
    {
        return available.get();
    }
    public void write(byte[] data)
    {
        if (!closed.get() && queue.offer(data))
            available.addAndGet(data.length);
    }
}
