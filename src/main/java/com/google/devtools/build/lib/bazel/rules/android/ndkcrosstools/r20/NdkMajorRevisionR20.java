// Copyright 2021 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.r20;

import com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.ApiLevel;
import com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.NdkMajorRevision;
import com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.NdkPaths;
import com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.StlImpl;
import com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools.r19.AndroidNdkCrosstoolsR19;
import com.google.devtools.build.lib.events.EventHandler;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CrosstoolRelease;

/** Logic specific to Android NDK R20. */
public class NdkMajorRevisionR20 implements NdkMajorRevision {
  private final String clangVersion;

  public NdkMajorRevisionR20(String clangVersion) {
    this.clangVersion = clangVersion;
  }

  @Override
  public CrosstoolRelease crosstoolRelease(
      NdkPaths ndkPaths, StlImpl stlImpl, String hostPlatform) {
    // NDK r20 is the same as r19 for bazel, other than API level differences below.
    return AndroidNdkCrosstoolsR19.create(ndkPaths, stlImpl, hostPlatform, clangVersion);
  }

  @Override
  public ApiLevel apiLevel(EventHandler eventHandler, String name, String apiLevel) {
    return new ApiLevelR20(eventHandler, name, apiLevel);
  }
}
