/*
 * Copyright 2016 Gleb Godonoga.
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
 */

package com.andrada.sitracker.events;

import com.andrada.sitracker.tasks.ImportAuthorsTask;

public class ImportUpdates {

    private final ImportAuthorsTask.ImportProgress importProgress;

    public ImportUpdates(ImportAuthorsTask.ImportProgress importProgress) {
        this.importProgress = importProgress;
    }

    public ImportAuthorsTask.ImportProgress getImportProgress() {
        return importProgress;
    }

    public boolean isFinished() {
        return importProgress != null && importProgress.getTotalAuthors() == importProgress.getTotalProcessed();
    }
}
