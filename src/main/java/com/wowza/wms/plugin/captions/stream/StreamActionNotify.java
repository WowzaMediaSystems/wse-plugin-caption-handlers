/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.stream;

import com.wowza.wms.stream.*;

public interface StreamActionNotify extends IMediaStreamActionNotify
{
	@Override 
	default void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset) {}

	@Override
	default void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {}

	@Override
	default void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {}

	@Override
	default void onPause(IMediaStream stream, boolean isPause, double location) {}

	@Override
	default void onSeek(IMediaStream stream, double location) {}

	@Override
	default void onStop(IMediaStream stream) {}
}
