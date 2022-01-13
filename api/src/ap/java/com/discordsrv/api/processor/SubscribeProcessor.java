/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.processor;

import com.discordsrv.api.event.bus.Subscribe;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * Annotation processor for {@link Subscribe}, gives a error during compilation if a given listener method is wrong.
 */
@SupportedAnnotationTypes(SubscribeProcessor.SUBSCRIBE_CLASS_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SubscribeProcessor extends AbstractProcessor {

    protected static final String SUBSCRIBE_CLASS_NAME = "com.discordsrv.api.event.bus.Subscribe";
    protected static final String EVENT_CLASS_NAME = "com.discordsrv.api.event.events.Event";
    protected static final String JDA_EVENT_CLASS_NAME = "net.dv8tion.jda.api.events.GenericEvent";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();

        TypeMirror eventMirror = types.getDeclaredType(elements.getTypeElement(EVENT_CLASS_NAME));
        TypeMirror jdaEventMirror = types.getDeclaredType(elements.getTypeElement(JDA_EVENT_CLASS_NAME));
        TypeElement subscribeType = elements.getTypeElement(SUBSCRIBE_CLASS_NAME);

        boolean hasAnnotation = false;
        for (TypeElement annotation : annotations) {
            if (annotation.equals(subscribeType)) {
                hasAnnotation = true;
                break;
            }
        }
        if (!hasAnnotation) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Subscribe.class)) {
            Messager messager = processingEnv.getMessager();
            if (element.getKind() != ElementKind.METHOD) {
                messager.printMessage(ERROR, "@Subscribe can only be used on methods", element);
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;
            if (method.getEnclosingElement().getKind().isInterface()) {
                messager.printMessage(ERROR, "Cannot be used on interfaces", element);
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                messager.printMessage(ERROR, "Must return void", element);
            }

            Set<Modifier> modifiers = method.getModifiers();
            if (!modifiers.contains(Modifier.PUBLIC)) {
                messager.printMessage(ERROR, "Must be public", element);
            }
            if (modifiers.contains(Modifier.STATIC)) {
                messager.printMessage(ERROR, "Must not be static", element);
            }
            if (modifiers.contains(Modifier.ABSTRACT)) {
                messager.printMessage(ERROR, "Cannot be abstract", element);
            }

            List<? extends VariableElement> parameters = method.getParameters();
            if (parameters.isEmpty()) {
                messager.printMessage(ERROR, "Method doesn't have a DiscordSRV or JDA event as the only parameter", element);
                continue;
            }
            if (parameters.size() > 1) {
                messager.printMessage(ERROR, "Method should only have the DiscordSRV or JDA event as a parameter", element);
            }

            TypeMirror firstParameter = parameters.get(0).asType();
            if (!types.isAssignable(firstParameter, eventMirror) && !types.isAssignable(firstParameter, jdaEventMirror)) {
                messager.printMessage(ERROR, "First argument is not a DiscordSRV or JDA event", element);
            }
        }
        return false;
    }
}
