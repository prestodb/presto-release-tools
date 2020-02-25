/*
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
package com.facebook.presto.release.tasks;

import com.facebook.airlift.bootstrap.Bootstrap;
import com.facebook.airlift.bootstrap.LifeCycleManager;
import com.facebook.airlift.log.Logger;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.airline.Option;

import javax.inject.Inject;

import java.lang.reflect.Field;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.lang.System.setProperty;

public abstract class AbstractReleaseCommand
        implements Runnable
{
    private static final Logger log = Logger.get(AbstractReleaseCommand.class);

    protected abstract List<Module> getModules();

    protected abstract Class<? extends ReleaseTask> getReleaseTask();

    @Override
    public final void run()
    {
        setConfigPropertiesFromOptions();

        Injector injector = null;
        try {
            injector = new Bootstrap(getModules()).strictConfig().initialize();
            injector.getInstance(getReleaseTask()).run();
        }
        catch (Exception e) {
            throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
        finally {
            if (injector != null) {
                try {
                    injector.getInstance(LifeCycleManager.class).stop();
                }
                catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    private void setConfigPropertiesFromOptions()
    {
        setConfigPropertiesFromOptions(this);
    }

    private static void setConfigPropertiesFromOptions(Object object)
    {
        for (Class<?> clazz = object.getClass(); !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                Inject inject = field.getAnnotation(Inject.class);
                if (inject != null) {
                    setConfigPropertiesFromOptions(getField(object, field));
                }

                Option option = field.getAnnotation(Option.class);
                if (option == null) {
                    continue;
                }

                ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
                checkState(configProperty != null, "@ConfigProperty annotation is required on fields annotated with @Option");
                checkState(!isNullOrEmpty(configProperty.value()), "@ConfigProperty value is not or empty");

                Object value = getField(object, field);
                if (option.required() || value != null) {
                    setProperty(configProperty.value(), value.toString());
                }
            }
        }
    }

    private static Object getField(Object object, Field field)
    {
        try {
            return field.get(object);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
