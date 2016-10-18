/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;

/**
 * AppEngineComponentPage is a page that displays a message that Gcloud App Engine Java component is missing
 * with instructions on how to install it. This page disables the 'Finish' button.
 */
public class AppEngineComponentPage extends WizardPage {

  protected AppEngineComponentPage() {
    super("appEngineComponentPage");
    setTitle("App Engine Component is missing");
    // TODO: do we need the description?
    setDescription("The Cloud SDK App Engine Java component is not installed"); 
  }

  @Override
  public void createControl(Composite parent) {
    // TODO: should we create a ping for missing app engine component
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE, parent.getShell());

    Composite container = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);

    Text text = new Text(container, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = parent.getSize().x;
    text.setLayoutData(gridData);
    text.setText(Messages.getString("appengine.java.component.missing"));
    text.setBackground(container.getBackground());

    setControl(container);
    setPageComplete(false);
    Dialog.applyDialogFont(container);
  }
  
}
