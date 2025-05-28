/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.stream;

import com.wowza.wms.mediacaster.*;
import com.wowza.wms.stream.*;

public interface MediaCasterNotify extends IMediaCasterNotify2
{
    @Override
    default void onConnectStart(IMediaCaster mediaCaster) {}

    @Override
    default void onConnectSuccess(IMediaCaster mediaCaster) {}

    @Override
    default void onConnectFailure(IMediaCaster mediaCaster) {}

    @Override
    default void onMediaCasterCreate(IMediaCaster mediaCaster) {}

    @Override
    default void onMediaCasterDestroy(IMediaCaster mediaCaster) {}

    @Override
    default void onRegisterPlayer(IMediaCaster mediaCaster, IMediaStreamPlay player) {}

    @Override
    default void onUnRegisterPlayer(IMediaCaster mediaCaster, IMediaStreamPlay player) {}

    @Override
    default void onSetSourceStream(IMediaCaster mediaCaster, IMediaStream stream) {}

    @Override
    default void onStreamStart(IMediaCaster mediaCaster) {}
}
