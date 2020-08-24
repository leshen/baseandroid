/*
 * Created by zhangxiangwei on 2019/11/05.
 * Copyright 2015－2020 Sl Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package baseandroid.sl.sdk.analytics;

public interface ThreadNameConstants {
    String THREAD_APP_END_DATA_SAVE_TIMER = "Sl.AppEndDataSaveTimerThread";
    String THREAD_TASK_QUEUE = "Sl.TaskQueueThread";
    String THREAD_SEND_DISTINCT_ID = "Sl.SendDistinctIDThread";
    String THREAD_GET_SDK_REMOTE_CONFIG = "Sl.GetSDKRemoteConfigThread";
    String THREAD_DEEP_LINK_REQUEST = "Sl.DeepLinkRequest";
}
