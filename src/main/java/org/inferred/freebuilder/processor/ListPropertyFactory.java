/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inferred.freebuilder.processor;

import static org.inferred.freebuilder.processor.Util.erasesToAnyOf;
import static org.inferred.freebuilder.processor.Util.upperBound;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.inferred.freebuilder.processor.Metadata.Property;
import org.inferred.freebuilder.processor.PropertyCodeGenerator.Config;
import org.inferred.freebuilder.processor.util.SourceBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * {@link PropertyCodeGenerator.Factory} providing append-only semantics for {@link List}
 * properties.
 */
public class ListPropertyFactory implements PropertyCodeGenerator.Factory {

  private static final String ADD_PREFIX = "add";
  private static final String ADD_ALL_PREFIX = "addAll";
  private static final String CLEAR_PREFIX = "clear";
  private static final String GET_PREFIX = "get";

  @Override
  public Optional<? extends PropertyCodeGenerator> create(Config config) {
    if (config.getProperty().getType().getKind() == TypeKind.DECLARED) {
      DeclaredType type = (DeclaredType) config.getProperty().getType();
      if (erasesToAnyOf(type, Collection.class, List.class, ImmutableList.class)) {
        TypeMirror elementType = upperBound(config.getElements(), type.getTypeArguments().get(0));
        Optional<TypeMirror> unboxedType;
        try {
          unboxedType = Optional.<TypeMirror>of(config.getTypes().unboxedType(elementType));
        } catch (IllegalArgumentException e) {
          unboxedType = Optional.absent();
        }
        return Optional.of(new CodeGenerator(config.getProperty(), elementType, unboxedType));
      }
    }
    return Optional.absent();
  }

  @VisibleForTesting static class CodeGenerator extends PropertyCodeGenerator {

    private final TypeMirror elementType;
    private final Optional<TypeMirror> unboxedType;

    @VisibleForTesting
    CodeGenerator(Property property, TypeMirror elementType, Optional<TypeMirror> unboxedType) {
      super(property);
      this.elementType = elementType;
      this.unboxedType = unboxedType;
    }

    @Override
    public void addBuilderFieldDeclaration(SourceBuilder code) {
      code.addLine("  private %1$s<%2$s> %3$s = new %1$s<%4$s>();",
          ArrayList.class,
          elementType,
          property.getName(),
          sourceLevel.supportsDiamondOperator() ? "" : elementType);
    }

    @Override
    public void addBuilderFieldAccessors(SourceBuilder code, Metadata metadata) {
      // add(T element)
      code.addLine("")
          .addLine("  /**")
          .addLine("   * Adds {@code element} to the list to be returned from {@link %s#%s()}.",
              metadata.getType(), property.getGetterName())
          .addLine("   *")
          .addLine("   * @return this {@code %s} object", metadata.getBuilder().getSimpleName());
      if (!unboxedType.isPresent()) {
        code.addLine("   * @throws NullPointerException if {@code element} is null");
      }
      code.addLine("   */")
          .addLine("  public %s %s%s(%s element) {",
              metadata.getBuilder(),
              ADD_PREFIX,
              property.getCapitalizedName(),
              unboxedType.or(elementType));
      if (unboxedType.isPresent()) {
        code.addLine("    this.%s.add(element);", property.getName());
      } else {
        code.addLine("    this.%s.add(%s.checkNotNull(element));",
            property.getName(), Preconditions.class);
      }
      code.addLine("    return (%s) this;", metadata.getBuilder())
          .addLine("  }");

      // add(T... elements)
      code.addLine("")
          .addLine("  /**")
          .addLine("   * Adds each element of {@code elements} to the list to be returned from")
          .addLine("   * {@link %s#%s()}.", metadata.getType(), property.getGetterName())
          .addLine("   *")
          .addLine("   * @return this {@code %s} object", metadata.getBuilder().getSimpleName());
      if (!unboxedType.isPresent()) {
        code.addLine("   * @throws NullPointerException if {@code elements} is null or contains a")
            .addLine("   *     null element");
      }
      code.addLine("   */")
          .addLine("  public %s %s%s(%s... elements) {",
              metadata.getBuilder(),
              ADD_PREFIX,
              property.getCapitalizedName(),
              unboxedType.or(elementType))
          .addLine("    %1$s.ensureCapacity(%1$s.size() + elements.length);", property.getName())
          .addLine("    for (%s element : elements) {", unboxedType.or(elementType))
          .addLine("      %s%s(element);", ADD_PREFIX, property.getCapitalizedName())
          .addLine("    }")
          .addLine("    return (%s) this;", metadata.getBuilder())
          .addLine("  }");

      // addAll(Iterable<? extends T> elements)
      code.addLine("")
          .addLine("  /**")
          .addLine("   * Adds each element of {@code elements} to the list to be returned from")
          .addLine("   * {@link %s#%s()}.", metadata.getType(), property.getGetterName())
          .addLine("   *")
          .addLine("   * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
          .addLine("   * @throws NullPointerException if {@code elements} is null or contains a")
          .addLine("   *     null element")
          .addLine("   */")
          .addLine("  public %s %s%s(%s<? extends %s> elements) {",
              metadata.getBuilder(),
              ADD_ALL_PREFIX,
              property.getCapitalizedName(),
              Iterable.class,
              elementType)
          .addLine("    if (elements instanceof %s) {", Collection.class)
          .addLine("      %1$s.ensureCapacity(%1$s.size() + ((%2$s<?>) elements).size());",
              property.getName(), Collection.class)
          .addLine("    }")
          .addLine("    for (%s element : elements) {", unboxedType.or(elementType))
          .addLine("      %s%s(element);", ADD_PREFIX, property.getCapitalizedName())
          .addLine("    }")
          .addLine("    return (%s) this;", metadata.getBuilder())
          .addLine("  }");

      // clear()
      code.addLine("")
          .addLine("  /**")
          .addLine("   * Clears the list to be returned from {@link %s#%s()}.",
              metadata.getType(), property.getGetterName())
          .addLine("   *")
          .addLine("   * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
          .addLine("   */")
          .addLine("  public %s %s%s() {",
              metadata.getBuilder(),
              CLEAR_PREFIX,
              property.getCapitalizedName())
          .addLine("    this.%s.clear();", property.getName())
          .addLine("    return (%s) this;", metadata.getBuilder())
          .addLine("  }");

      // get()
      code.addLine("")
          .addLine("  /**")
          .addLine("   * Returns an unmodifiable view of the list that will be returned by")
          .addLine("   * {@link %s#%s()}.", metadata.getType(), property.getGetterName())
          .addLine("   * Changes to this builder will be reflected in the view.")
          .addLine("   */")
          .addLine("  public %s<%s> %s%s() {",
              List.class,
              elementType,
              GET_PREFIX,
              property.getCapitalizedName())
          .addLine("    return %s.unmodifiableList(%s);", Collections.class, property.getName())
          .addLine("  }");
    }

    @Override
    public void addFinalFieldAssignment(SourceBuilder code, String finalField, String builder) {
      code.addLine("      %s = %s.copyOf(%s.%s);",
          finalField, ImmutableList.class, builder, property.getName());
    }

    @Override
    public void addMergeFromValue(SourceBuilder code, String value) {
      code.addLine("    %s%s(%s.%s());",
          ADD_ALL_PREFIX, property.getCapitalizedName(), value, property.getGetterName());
    }

    @Override
    public void addMergeFromBuilder(SourceBuilder code, Metadata metadata, String builder) {
      code.addLine("    %s%s(((%s) %s).%s);",
          ADD_ALL_PREFIX,
          property.getCapitalizedName(),
          metadata.getGeneratedBuilder(),
          builder,
          property.getName());
    }

    @Override
    public void addSetFromResult(SourceBuilder code, String builder, String variable) {
      code.addLine("        %s.%s%s(%s);",
          builder, ADD_ALL_PREFIX, property.getCapitalizedName(), variable);
    }

    @Override
    public boolean isTemplateRequiredInClear() {
      return false;
    }

    @Override
    public void addClear(SourceBuilder code, String template) {
      code.addLine("    %s.clear();", property.getName());
    }

    @Override
    public void addPartialClear(SourceBuilder code) {
      code.addLine("    %s.clear();", property.getName());
    }
  }
}
