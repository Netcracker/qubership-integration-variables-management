/*
 * Copyright 2024-2025 NetCracker Technology Corporation
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

package org.qubership.integration.platform.variables.management.model.exportimport.instructions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record PerformInstructionsResult(
        Set<String> variablesToIgnore,
        List<ImportInstructionsExecutionResult> importInstructionsExecutionResults
) {
    public PerformInstructionsResult {
        if (variablesToIgnore == null) {
            variablesToIgnore = new HashSet<>();
        }
        if (importInstructionsExecutionResults == null) {
            importInstructionsExecutionResults = new ArrayList<>();
        }
    }
}
