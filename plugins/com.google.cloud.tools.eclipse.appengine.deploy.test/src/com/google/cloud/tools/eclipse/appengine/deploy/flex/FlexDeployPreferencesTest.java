/*******************************************************************************
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.flex;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FlexDeployPreferencesTest {
  @Mock private IEclipsePreferences mockEclipsePreferences;

  @Test
  public void testDefaultAppEngineConfigFolder() {
    assertThat(FlexDeployPreferences.DEFAULT.getAppEngineConfigFolder(), isEmptyString());
  }

  @Test
  public void testDefaultDockerFileLocation() {
    assertThat(FlexDeployPreferences.DEFAULT.getDockerFileLocation(), isEmptyString());
  }

  @Test
  public void testDefaultUseDeploymentPreferences() {
    assertFalse(FlexDeployPreferences.DEFAULT.getUseDeploymentPreferences());
  }

  @Test
  public void testGetAppEngineConfigFolder() {
    when(mockEclipsePreferences.get(eq(FlexDeployPreferences.PREF_APP_ENGINE_CONFIG_FOLDER), anyString()))
      .thenReturn("configFolder");
    FlexDeployPreferences flexDeployPreferences = new FlexDeployPreferences(mockEclipsePreferences);
    assertThat(flexDeployPreferences.getAppEngineConfigFolder(), is("configFolder"));
  }

  @Test
  public void testSetAppEngineConfigFolder() {
    FlexDeployPreferences flexDeployPreferences = new FlexDeployPreferences(mockEclipsePreferences);
    flexDeployPreferences.setAppEngineConfigFolder("configFolder");
    verify(mockEclipsePreferences, times(1)).put(eq(FlexDeployPreferences.PREF_APP_ENGINE_CONFIG_FOLDER), eq("configFolder"));
  }

  @Test
  public void testGetDockerFileLocation() {
    when(mockEclipsePreferences.get(eq(FlexDeployPreferences.PREF_DOCKER_FILE_LOCATION), anyString()))
      .thenReturn("dockerFileLocation");
    FlexDeployPreferences flexDeployPreferences = new FlexDeployPreferences(mockEclipsePreferences);
    assertThat(flexDeployPreferences.getDockerFileLocation(), is("dockerFileLocation"));
  }

  @Test
  public void testSetDockerFileLocation() {
    FlexDeployPreferences flexDeployPreferences = new FlexDeployPreferences(mockEclipsePreferences);
    flexDeployPreferences.setDockerFileLocation("dockerFileLocation");
    verify(mockEclipsePreferences, times(1)).put(eq(FlexDeployPreferences.PREF_DOCKER_FILE_LOCATION), eq("dockerFileLocation"));
  }

  @Test
  public void testGetUseDeploymentPreferences() {
    when(mockEclipsePreferences.getBoolean(eq(FlexDeployPreferences.PREF_USE_DEPLOYMENT_PREFERENCES), anyBoolean()))
      .thenReturn(false);
    FlexDeployPreferences flexDeployPreferences = new FlexDeployPreferences(mockEclipsePreferences);
    assertFalse(flexDeployPreferences.getUseDeploymentPreferences());
  }

  @Test
  public void testSetUseDeploymentPreferences() {
    FlexDeployPreferences flexDeployPreferences = new FlexDeployPreferences(mockEclipsePreferences);
    flexDeployPreferences.setUseDeploymentPreferences(true);
    verify(mockEclipsePreferences, times(1)).putBoolean(eq(FlexDeployPreferences.PREF_USE_DEPLOYMENT_PREFERENCES), eq(true));
  }
}
