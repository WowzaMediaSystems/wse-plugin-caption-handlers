/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.audio;

public interface SpeechHandler extends Runnable, AutoCloseable
{
    void addAudioFrame(byte[] frame);
    void close();
}
