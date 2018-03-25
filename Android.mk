#
# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

#
# Build rule for Launcher3 app.
#
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v7-recyclerview \
    android-support-v7-palette \
    android-support-dynamic-animation

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    $(call all-java-files-under, src_config) \
    $(call all-java-files-under, src_flags) \
    $(call all-Iaidl-files-under, src) \
    $(call all-proto-files-under, protos) \
    $(call all-proto-files-under, proto_overrides) \
    $(call all-proto-files-under, proto_pixel)

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res \
    prebuilts/sdk/current/support/v7/recyclerview/res \

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTOC_FLAGS := --proto_path=$(LOCAL_PATH)/protos/ --proto_path=$(LOCAL_PATH)/proto_overrides/ --proto_path=$(LOCAL_PATH)/proto_pixel/
LOCAL_PROTO_JAVA_OUTPUT_PARAMS := enum_style=java

LOCAL_AAPT_FLAGS := \
    --rename-manifest-package com.google.android.apps.nexuslauncher \
    --auto-add-overlay \
    --extra-packages android.support.v7.recyclerview \

LOCAL_SDK_VERSION := current
LOCAL_MIN_SDK_VERSION := 21
LOCAL_PACKAGE_NAME := RootlessPixelLauncher
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := Home Launcher2

LOCAL_FULL_LIBS_MANIFEST_FILES := $(LOCAL_PATH)/AndroidManifest-common.xml

LOCAL_JACK_COVERAGE_INCLUDE_FILTER := com.android.launcher3.*

include $(BUILD_PACKAGE)

#
# Build rule for Launcher3 Go app for Android Go devices.
#
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v7-recyclerview \
    android-support-v7-palette \
    android-support-dynamic-animation

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    $(call all-java-files-under, src_config) \
    $(call all-java-files-under, go/src_flags) \
    $(call all-proto-files-under, protos) \
    $(call all-proto-files-under, proto_overrides)

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/go/res \
    $(LOCAL_PATH)/res \
    prebuilts/sdk/current/support/v7/recyclerview/res \

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTOC_FLAGS := --proto_path=$(LOCAL_PATH)/protos/ --proto_path=$(LOCAL_PATH)/proto_overrides/
LOCAL_PROTO_JAVA_OUTPUT_PARAMS := enum_style=java

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.recyclerview \

LOCAL_SDK_VERSION := current
LOCAL_MIN_SDK_VERSION := 21
LOCAL_PACKAGE_NAME := Launcher3Go
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := Home Launcher2 Launcher3

LOCAL_FULL_LIBS_MANIFEST_FILES := \
    $(LOCAL_PATH)/AndroidManifest.xml \
    $(LOCAL_PATH)/AndroidManifest-common.xml

LOCAL_MANIFEST_FILE := go/AndroidManifest.xml

LOCAL_JACK_COVERAGE_INCLUDE_FILTER := com.android.launcher3.*

include $(BUILD_PACKAGE)

#
# Launcher proto buffer jar used for development
#
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-proto-files-under, protos) $(call all-proto-files-under, proto_overrides) \
    $(call all-proto-files-under, proto_pixel)

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTOC_FLAGS := --proto_path=$(LOCAL_PATH)/protos/ --proto_path=$(LOCAL_PATH)/proto_overrides/ --proto_path=$(LOCAL_PATH)/proto_pixel/
LOCAL_PROTO_JAVA_OUTPUT_PARAMS := enum_style=java

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := launcher_proto_lib
LOCAL_IS_HOST_MODULE := true
LOCAL_STATIC_JAVA_LIBRARIES := host-libprotobuf-java-nano

include $(BUILD_HOST_JAVA_LIBRARY)

# ==================================================
include $(call all-makefiles-under,$(LOCAL_PATH))
