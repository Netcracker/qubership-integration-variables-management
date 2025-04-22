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

package org.qubership.integration.platform.variables.management.configuration.datasource.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.internal.configuration.ConfigUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@ConfigurationProperties(prefix = "db")
public class FlywayConfigProperties {
    private Map<String, Properties> flyway;

    public ClassicConfiguration getConfig(String name) {
        return Optional.ofNullable(flyway.get(name)).map(props -> {
            ClassicConfiguration configuration = new ClassicConfiguration();
            configuration.configure(getConfigurationMap(props));
            return configuration;
        }).orElseGet(ClassicConfiguration::new);
    }

    private Map<String, String> getConfigurationMap(Properties properties) {
        return addPrefix("flyway.", ConfigUtils.propertiesToMap(properties));
    }

    private Map<String, String> addPrefix(String prefix, Map<String, String> map) {
        return map.entrySet().stream().collect(Collectors.toMap(e -> prefix + e.getKey(), Map.Entry::getValue));
    }
}
