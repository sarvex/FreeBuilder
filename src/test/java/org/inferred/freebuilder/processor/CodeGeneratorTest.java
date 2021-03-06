/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import static com.google.common.truth.Truth.assertThat;
import static org.inferred.freebuilder.processor.util.ClassTypeImpl.newNestedClass;
import static org.inferred.freebuilder.processor.util.ClassTypeImpl.newTopLevelClass;
import static org.inferred.freebuilder.processor.util.PrimitiveTypeImpl.INT;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.inferred.freebuilder.processor.Metadata.Property;
import org.inferred.freebuilder.processor.util.ImpliedClass;
import org.inferred.freebuilder.processor.util.SourceStringBuilder;
import org.inferred.freebuilder.processor.util.ClassTypeImpl;
import org.inferred.freebuilder.processor.util.NameImpl;
import org.inferred.freebuilder.processor.util.NoTypes;
import org.inferred.freebuilder.processor.util.PackageElementImpl;
import org.inferred.freebuilder.processor.util.ValueType;
import org.inferred.freebuilder.processor.CodeGeneratorTest.GenericTypeElementImpl.GenericTypeMirrorImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.cglib.proxy.CallbackHelper;
import org.mockito.cglib.proxy.Enhancer;
import org.mockito.cglib.proxy.InvocationHandler;
import org.mockito.cglib.proxy.NoOp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.AbstractElementVisitor6;
import javax.lang.model.util.Elements;

@RunWith(JUnit4.class)
public class CodeGeneratorTest {

  private static final PackageElement PACKAGE = new PackageElementImpl("com.example");

  @Test
  public void testSimpleDataType() {
    TypeElement person = newTopLevelClass("com.example.Person").asElement();
    TypeMirror string = newTopLevelClass("java.lang.String");
    ImpliedClass generatedBuilder =
        new ImpliedClass(PACKAGE, "Person_Builder", person, elements());
    Property.Builder name = new Property.Builder()
        .setAllCapsName("NAME")
        .setBoxedType(string)
        .setCapitalizedName("Name")
        .setFullyCheckedCast(true)
        .setGetterName("getName")
        .setName("name")
        .setType(string);
    Property.Builder age = new Property.Builder()
        .setAllCapsName("AGE")
        .setBoxedType(newTopLevelClass("java.lang.Integer"))
        .setCapitalizedName("Age")
        .setFullyCheckedCast(true)
        .setGetterName("getAge")
        .setName("age")
        .setType(INT);
    Metadata metadata = new Metadata.Builder(elements())
        .setBuilder(newNestedClass(person, "Builder").asElement())
        .setBuilderFactory(BuilderFactory.NO_ARGS_CONSTRUCTOR)
        .setBuilderSerializable(false)
        .setGeneratedBuilder(generatedBuilder)
        .setGwtCompatible(false)
        .setGwtSerializable(false)
        .setPartialType(generatedBuilder.createNestedClass("Partial"))
        .addProperty(name
            .setCodeGenerator(
                new DefaultPropertyFactory.CodeGenerator(name.build(), "setName", false))
            .build())
        .addProperty(age
            .setCodeGenerator(
                new DefaultPropertyFactory.CodeGenerator(age.build(), "setAge", false))
            .build())
        .setPropertyEnum(generatedBuilder.createNestedClass("Property"))
        .setType(person)
        .setValueType(generatedBuilder.createNestedClass("Value"))
        .build();

    SourceStringBuilder sourceBuilder = SourceStringBuilder.simple();
    new CodeGenerator().writeBuilderSource(sourceBuilder, metadata);

    assertThat(sourceBuilder.toString()).isEqualTo(Joiner.on('\n').join(
        "/**",
        " * Auto-generated superclass of {@link Person.Builder},",
        " * derived from the API of {@link Person}.",
        " */",
        "@Generated(\"org.inferred.freebuilder.processor.CodeGenerator\")",
        "abstract class Person_Builder {",
        "",
        "  private static final Joiner COMMA_JOINER = Joiner.on(\", \").skipNulls();",
        "",
        "  private enum Property {",
        "    NAME(\"name\"),",
        "    AGE(\"age\"),",
        "    ;",
        "",
        "    private final String name;",
        "",
        "    private Property(String name) {",
        "      this.name = name;",
        "    }",
        "",
        "    @Override public String toString() {",
        "      return name;",
        "    }",
        "  }",
        "",
        "  private String name;",
        "  private int age;",
        "  private final EnumSet<Person_Builder.Property> _unsetProperties =",
        "      EnumSet.allOf(Person_Builder.Property.class);",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code name} is null",
        "   */",
        "  public Person.Builder setName(String name) {",
        "    this.name = Preconditions.checkNotNull(name);",
        "    _unsetProperties.remove(Person_Builder.Property.NAME);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getName()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public String getName() {",
        "    Preconditions.checkState(",
        "        !_unsetProperties.contains(Person_Builder.Property.NAME),",
        "        \"name not set\");",
        "    return name;",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setAge(int age) {",
        "    this.age = age;",
        "    _unsetProperties.remove(Person_Builder.Property.AGE);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getAge()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public int getAge() {",
        "    Preconditions.checkState(",
        "        !_unsetProperties.contains(Person_Builder.Property.AGE),",
        "        \"age not set\");",
        "    return age;",
        "  }",
        "",
        "  private static final class Value extends Person {",
        "    private final String name;",
        "    private final int age;",
        "",
        "    private Value(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Value)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Value other = (Person_Builder.Value) obj;",
        "      if (!name.equals(other.name)) {",
        "        return false;",
        "      }",
        "      if (age != other.age) {",
        "        return false;",
        "      }",
        "      return true;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Arrays.hashCode(new Object[] { name, age });",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"Person{\"",
        "          + \"name=\" + name + \", \"",
        "          + \"age=\" + age + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created {@link Person} based on the contents of the {@code Builder}.",
        "   *",
        "   * @throws IllegalStateException if any field has not been set",
        "   */",
        "  public Person build() {",
        "    Preconditions.checkState(_unsetProperties.isEmpty(),"
            + " \"Not set: %s\", _unsetProperties);",
        "    return new Person_Builder.Value(this);",
        "  }",
        "",
        "  /**",
        "   * Sets all property values using the given {@code Person} as a template.",
        "   */",
        "  public Person.Builder mergeFrom(Person value) {",
        "    setName(value.getName());",
        "    setAge(value.getAge());",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Copies values from the given {@code Builder}.",
        "   * Does not affect any properties not set on the input.",
        "   */",
        "  public Person.Builder mergeFrom(Person.Builder template) {",
        "    // Upcast to access the private _unsetProperties field.",
        "    // Otherwise, oddly, we get an access violation.",
        "    EnumSet<Person_Builder.Property> _templateUnset = ((Person_Builder) template)"
            + "._unsetProperties;",
        "    if (!_templateUnset.contains(Person_Builder.Property.NAME)) {",
        "      setName(template.getName());",
        "    }",
        "    if (!_templateUnset.contains(Person_Builder.Property.AGE)) {",
        "      setAge(template.getAge());",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Resets the state of this builder.",
        "   */",
        "  public Person.Builder clear() {",
        "    Person_Builder template = new Person.Builder();",
        "    name = template.name;",
        "    age = template.age;",
        "    _unsetProperties.clear();",
        "    _unsetProperties.addAll(template._unsetProperties);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  private static final class Partial extends Person {",
        "    private final String name;",
        "    private final int age;",
        "    private final EnumSet<Person_Builder.Property> _unsetProperties;",
        "",
        "    Partial(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "      this._unsetProperties = builder._unsetProperties.clone();",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "        throw new UnsupportedOperationException(\"name not set\");",
        "      }",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "        throw new UnsupportedOperationException(\"age not set\");",
        "      }",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Partial)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Partial other = (Person_Builder.Partial) obj;",
        "      if (name != other.name",
        "          && (name == null || !name.equals(other.name))) {",
        "        return false;",
        "      }",
        "      if (age != other.age) {",
        "        return false;",
        "      }",
        "      return _unsetProperties.equals(other._unsetProperties);",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      int result = 1;",
        "      result *= 31;",
        "      result += ((name == null) ? 0 : name.hashCode());",
        "      result *= 31;",
        "      result += ((Integer) age).hashCode();",
        "      result *= 31;",
        "      result += _unsetProperties.hashCode();",
        "      return result;",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"partial Person{\"",
        "          + COMMA_JOINER.join(",
        "              (!_unsetProperties.contains(Person_Builder.Property.NAME)",
        "                  ? \"name=\" + name : null),",
        "              (!_unsetProperties.contains(Person_Builder.Property.AGE)",
        "                  ? \"age=\" + age : null))",
        "          + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created partial {@link Person}",
        "   * based on the contents of the {@code Builder}.",
        "   * State checking will not be performed.",
        "   * Unset properties will throw an {@link UnsupportedOperationException}",
        "   * when accessed via the partial object.",
        "   *",
        "   * <p>Partials should only ever be used in tests.",
        "   */",
        "  @VisibleForTesting()",
        "  public Person buildPartial() {",
        "    return new Person_Builder.Partial(this);",
        "  }",
        "}\n"));
  }

  @Test
  public void testNoRequiredProperties() {
    TypeElement person = newTopLevelClass("com.example.Person").asElement();
    TypeMirror string = newTopLevelClass("java.lang.String");
    ImpliedClass generatedBuilder =
        new ImpliedClass(PACKAGE, "Person_Builder", person, elements());
    Property.Builder name = new Property.Builder()
        .setAllCapsName("NAME")
        .setBoxedType(string)
        .setCapitalizedName("Name")
        .setFullyCheckedCast(true)
        .setGetterName("getName")
        .setName("name")
        .setType(string);
    Property.Builder age = new Property.Builder()
        .setAllCapsName("AGE")
        .setBoxedType(newTopLevelClass("java.lang.Integer"))
        .setCapitalizedName("Age")
        .setFullyCheckedCast(true)
        .setGetterName("getAge")
        .setName("age")
        .setType(INT);
    Metadata metadata = new Metadata.Builder(elements())
        .setBuilder(newNestedClass(person, "Builder").asElement())
        .setBuilderFactory(BuilderFactory.NO_ARGS_CONSTRUCTOR)
        .setBuilderSerializable(false)
        .setGeneratedBuilder(generatedBuilder)
        .setGwtCompatible(false)
        .setGwtSerializable(false)
        .setPartialType(generatedBuilder.createNestedClass("Partial"))
        .addProperty(name
            .setCodeGenerator(
                new DefaultPropertyFactory.CodeGenerator(name.build(), "setName", true))
            .build())
        .addProperty(age
            .setCodeGenerator(
                new DefaultPropertyFactory.CodeGenerator(age.build(), "setAge", true))
            .build())
        .setPropertyEnum(generatedBuilder.createNestedClass("Property"))
        .setType(person)
        .setValueType(generatedBuilder.createNestedClass("Value"))
        .build();

    SourceStringBuilder sourceBuilder = SourceStringBuilder.simple();
    new CodeGenerator().writeBuilderSource(sourceBuilder, metadata);

    assertThat(sourceBuilder.toString()).isEqualTo(Joiner.on('\n').join(
        "/**",
        " * Auto-generated superclass of {@link Person.Builder},",
        " * derived from the API of {@link Person}.",
        " */",
        "@Generated(\"org.inferred.freebuilder.processor.CodeGenerator\")",
        "abstract class Person_Builder {",
        "",
        "  private static final Joiner COMMA_JOINER = Joiner.on(\", \").skipNulls();",
        "",
        "  private String name;",
        "  private int age;",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code name} is null",
        "   */",
        "  public Person.Builder setName(String name) {",
        "    this.name = Preconditions.checkNotNull(name);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getName()}.",
        "   */",
        "  public String getName() {",
        "    return name;",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setAge(int age) {",
        "    this.age = age;",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getAge()}.",
        "   */",
        "  public int getAge() {",
        "    return age;",
        "  }",
        "",
        "  private static final class Value extends Person {",
        "    private final String name;",
        "    private final int age;",
        "",
        "    private Value(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Value)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Value other = (Person_Builder.Value) obj;",
        "      if (!name.equals(other.name)) {",
        "        return false;",
        "      }",
        "      if (age != other.age) {",
        "        return false;",
        "      }",
        "      return true;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Arrays.hashCode(new Object[] { name, age });",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"Person{\"",
        "          + \"name=\" + name + \", \"",
        "          + \"age=\" + age + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created {@link Person} based on the contents of the {@code Builder}.",
        "   */",
        "  public Person build() {",
        "    return new Person_Builder.Value(this);",
        "  }",
        "",
        "  /**",
        "   * Sets all property values using the given {@code Person} as a template.",
        "   */",
        "  public Person.Builder mergeFrom(Person value) {",
        "    setName(value.getName());",
        "    setAge(value.getAge());",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Copies values from the given {@code Builder}.",
        "   */",
        "  public Person.Builder mergeFrom(Person.Builder template) {",
        "    setName(template.getName());",
        "    setAge(template.getAge());",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Resets the state of this builder.",
        "   */",
        "  public Person.Builder clear() {",
        "    Person_Builder template = new Person.Builder();",
        "    name = template.name;",
        "    age = template.age;",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  private static final class Partial extends Person {",
        "    private final String name;",
        "    private final int age;",
        "",
        "    Partial(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Partial)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Partial other = (Person_Builder.Partial) obj;",
        "      if (!name.equals(other.name)) {",
        "        return false;",
        "      }",
        "      if (age != other.age) {",
        "        return false;",
        "      }",
        "      return true;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      int result = 1;",
        "      result *= 31;",
        "      result += ((name == null) ? 0 : name.hashCode());",
        "      result *= 31;",
        "      result += ((Integer) age).hashCode();",
        "      return result;",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"partial Person{\"",
        "          + COMMA_JOINER.join(",
        "              \"name=\" + name,",
        "              \"age=\" + age)",
        "          + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created partial {@link Person}",
        "   * based on the contents of the {@code Builder}.",
        "   * State checking will not be performed.",
        "   *",
        "   * <p>Partials should only ever be used in tests.",
        "   */",
        "  @VisibleForTesting()",
        "  public Person buildPartial() {",
        "    return new Person_Builder.Partial(this);",
        "  }",
        "}\n"));
  }

  @Test
  public void testOptionalProperties() {
    GenericTypeElementImpl optional = newTopLevelGenericType("com.google.common.base.Optional");
    ClassTypeImpl integer = newTopLevelClass("java.lang.Integer");
    GenericTypeMirrorImpl optionalInteger = optional.newMirror(integer);
    ClassTypeImpl string = newTopLevelClass("java.lang.String");
    GenericTypeMirrorImpl optionalString = optional.newMirror(string);
    TypeElement person = newTopLevelClass("com.example.Person").asElement();
    ImpliedClass generatedBuilder =
        new ImpliedClass(PACKAGE, "Person_Builder", person, elements());
    Property.Builder name = new Property.Builder()
        .setAllCapsName("NAME")
        .setBoxedType(optionalString)
        .setCapitalizedName("Name")
        .setFullyCheckedCast(true)
        .setGetterName("getName")
        .setName("name")
        .setType(optionalString);
    Property.Builder age = new Property.Builder()
        .setAllCapsName("AGE")
        .setBoxedType(optionalInteger)
        .setCapitalizedName("Age")
        .setFullyCheckedCast(true)
        .setGetterName("getAge")
        .setName("age")
        .setType(optionalInteger);
    Metadata metadata = new Metadata.Builder(elements())
        .setBuilder(newNestedClass(person, "Builder").asElement())
        .setBuilderFactory(BuilderFactory.NO_ARGS_CONSTRUCTOR)
        .setBuilderSerializable(false)
        .setGeneratedBuilder(generatedBuilder)
        .setGwtCompatible(false)
        .setGwtSerializable(false)
        .setPartialType(generatedBuilder.createNestedClass("Partial"))
        .addProperty(name
            .setCodeGenerator(new OptionalPropertyFactory.CodeGenerator(
                name.build(), "setName", "setNullableName", "clearName", string,
                Optional.<TypeMirror>absent()))
            .build())
        .addProperty(age
            .setCodeGenerator(new OptionalPropertyFactory.CodeGenerator(
                age.build(), "setAge", "setNullableAge", "clearAge", integer,
                Optional.<TypeMirror>of(INT)))
            .build())
        .setPropertyEnum(generatedBuilder.createNestedClass("Property"))
        .setType(person)
        .setValueType(generatedBuilder.createNestedClass("Value"))
        .build();

    SourceStringBuilder sourceBuilder = SourceStringBuilder.simple();
    new CodeGenerator().writeBuilderSource(sourceBuilder, metadata);

    assertThat(sourceBuilder.toString()).isEqualTo(Joiner.on('\n').join(
        "/**",
        " * Auto-generated superclass of {@link Person.Builder},",
        " * derived from the API of {@link Person}.",
        " */",
        "@Generated(\"org.inferred.freebuilder.processor.CodeGenerator\")",
        "abstract class Person_Builder {",
        "",
        "  private static final Joiner COMMA_JOINER = Joiner.on(\", \").skipNulls();",
        "",
        "  // Store a nullable object instead of an Optional. Escape analysis then",
        "  // allows the JVM to optimize away the Optional objects created by and",
        "  // passed to our API.",
        "  private String name = null;",
        "  // Store a nullable object instead of an Optional. Escape analysis then",
        "  // allows the JVM to optimize away the Optional objects created by and",
        "  // passed to our API.",
        "  private Integer age = null;",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code name} is null",
        "   */",
        "  public Person.Builder setName(String name) {",
        "    this.name = Preconditions.checkNotNull(name);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setName(Optional<? extends String> name) {",
        "    if (name.isPresent()) {",
        "      return setName(name.get());",
        "    } else {",
        "      return clearName();",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setNullableName(@Nullable String name) {",
        "    if (name != null) {",
        "      return setName(name);",
        "    } else {",
        "      return clearName();",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}",
        "   * to {@link Optional#absent() Optional.absent()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder clearName() {",
        "    this.name = null;",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getName()}.",
        "   */",
        "  public Optional<String> getName() {",
        "    return Optional.fromNullable(name);",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setAge(int age) {",
        "    this.age = age;",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setAge(Optional<? extends Integer> age) {",
        "    if (age.isPresent()) {",
        "      return setAge(age.get());",
        "    } else {",
        "      return clearAge();",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setNullableAge(@Nullable Integer age) {",
        "    if (age != null) {",
        "      return setAge(age);",
        "    } else {",
        "      return clearAge();",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}",
        "   * to {@link Optional#absent() Optional.absent()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder clearAge() {",
        "    this.age = null;",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getAge()}.",
        "   */",
        "  public Optional<Integer> getAge() {",
        "    return Optional.fromNullable(age);",
        "  }",
        "",
        "  private static final class Value extends Person {",
        "    // Store a nullable object instead of an Optional. Escape analysis then",
        "    // allows the JVM to optimize away the Optional objects created by our",
        "    // getter method.",
        "    private final String name;",
        "    // Store a nullable object instead of an Optional. Escape analysis then",
        "    // allows the JVM to optimize away the Optional objects created by our",
        "    // getter method.",
        "    private final Integer age;",
        "",
        "    private Value(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public Optional<String> getName() {",
        "      return Optional.fromNullable(name);",
        "    }",
        "",
        "    @Override",
        "    public Optional<Integer> getAge() {",
        "      return Optional.fromNullable(age);",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Value)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Value other = (Person_Builder.Value) obj;",
        "      if (name != other.name",
        "          && (name == null || !name.equals(other.name))) {",
        "        return false;",
        "      }",
        "      if (age != other.age",
        "          && (age == null || !age.equals(other.age))) {",
        "        return false;",
        "      }",
        "      return true;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Arrays.hashCode(new Object[] { name, age });",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"Person{\"",
        "          + COMMA_JOINER.join(",
        "              (name != null ? \"name=\" + name : null),",
        "              (age != null ? \"age=\" + age : null))",
        "          + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created {@link Person} based on the contents of the {@code Builder}.",
        "   */",
        "  public Person build() {",
        "    return new Person_Builder.Value(this);",
        "  }",
        "",
        "  /**",
        "   * Sets all property values using the given {@code Person} as a template.",
        "   */",
        "  public Person.Builder mergeFrom(Person value) {",
        "    setName(value.getName());",
        "    setAge(value.getAge());",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Copies values from the given {@code Builder}.",
        "   */",
        "  public Person.Builder mergeFrom(Person.Builder template) {",
        "    setName(template.getName());",
        "    setAge(template.getAge());",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Resets the state of this builder.",
        "   */",
        "  public Person.Builder clear() {",
        "    Person_Builder template = new Person.Builder();",
        "    name = template.name;",
        "    age = template.age;",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  private static final class Partial extends Person {",
        "    // Store a nullable object instead of an Optional. Escape analysis then",
        "    // allows the JVM to optimize away the Optional objects created by our",
        "    // getter method.",
        "    private final String name;",
        "    // Store a nullable object instead of an Optional. Escape analysis then",
        "    // allows the JVM to optimize away the Optional objects created by our",
        "    // getter method.",
        "    private final Integer age;",
        "",
        "    Partial(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public Optional<String> getName() {",
        "      return Optional.fromNullable(name);",
        "    }",
        "",
        "    @Override",
        "    public Optional<Integer> getAge() {",
        "      return Optional.fromNullable(age);",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Partial)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Partial other = (Person_Builder.Partial) obj;",
        "      if (name != other.name",
        "          && (name == null || !name.equals(other.name))) {",
        "        return false;",
        "      }",
        "      if (age != other.age",
        "          && (age == null || !age.equals(other.age))) {",
        "        return false;",
        "      }",
        "      return true;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      int result = 1;",
        "      result *= 31;",
        "      result += ((name == null) ? 0 : name.hashCode());",
        "      result *= 31;",
        "      result += ((age == null) ? 0 : age.hashCode());",
        "      return result;",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"partial Person{\"",
        "          + COMMA_JOINER.join(",
        "              (name != null ? \"name=\" + name : null),",
        "              (age != null ? \"age=\" + age : null))",
        "          + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created partial {@link Person}",
        "   * based on the contents of the {@code Builder}.",
        "   * State checking will not be performed.",
        "   *",
        "   * <p>Partials should only ever be used in tests.",
        "   */",
        "  @VisibleForTesting()",
        "  public Person buildPartial() {",
        "    return new Person_Builder.Partial(this);",
        "  }",
        "}\n"));
  }

  @Test
  public void testListProperties() {
    GenericTypeElementImpl list = newTopLevelGenericType("com.google.common.base.List");
    ClassTypeImpl integer = newTopLevelClass("java.lang.Integer");
    GenericTypeMirrorImpl listInteger = list.newMirror(integer);
    ClassTypeImpl string = newTopLevelClass("java.lang.String");
    GenericTypeMirrorImpl listString = list.newMirror(string);
    TypeElement person = newTopLevelClass("com.example.Person").asElement();
    ImpliedClass generatedBuilder =
        new ImpliedClass(PACKAGE, "Person_Builder", person, elements());
    Property.Builder name = new Property.Builder()
        .setAllCapsName("NAME")
        .setBoxedType(listString)
        .setCapitalizedName("Name")
        .setFullyCheckedCast(true)
        .setGetterName("getName")
        .setName("name")
        .setType(listString);
    Property.Builder age = new Property.Builder()
        .setAllCapsName("AGE")
        .setBoxedType(listInteger)
        .setCapitalizedName("Age")
        .setFullyCheckedCast(true)
        .setGetterName("getAge")
        .setName("age")
        .setType(listInteger);
    Metadata metadata = new Metadata.Builder(elements())
        .setBuilder(newNestedClass(person, "Builder").asElement())
        .setBuilderFactory(BuilderFactory.NO_ARGS_CONSTRUCTOR)
        .setBuilderSerializable(false)
        .setGeneratedBuilder(generatedBuilder)
        .setGwtCompatible(false)
        .setGwtSerializable(false)
        .setPartialType(generatedBuilder.createNestedClass("Partial"))
        .addProperty(name
            .setCodeGenerator(new ListPropertyFactory.CodeGenerator(
                name.build(), string, Optional.<TypeMirror>absent()))
            .build())
        .addProperty(age
            .setCodeGenerator(new ListPropertyFactory.CodeGenerator(
                age.build(), integer, Optional.<TypeMirror>of(INT)))
            .build())
        .setPropertyEnum(generatedBuilder.createNestedClass("Property"))
        .setType(person)
        .setValueType(generatedBuilder.createNestedClass("Value"))
        .build();

    SourceStringBuilder sourceBuilder = SourceStringBuilder.simple();
    new CodeGenerator().writeBuilderSource(sourceBuilder, metadata);

    assertThat(sourceBuilder.toString()).isEqualTo(Joiner.on('\n').join(
        "/**",
        " * Auto-generated superclass of {@link Person.Builder},",
        " * derived from the API of {@link Person}.",
        " */",
        "@Generated(\"org.inferred.freebuilder.processor.CodeGenerator\")",
        "abstract class Person_Builder {",
        "",
        "  private static final Joiner COMMA_JOINER = Joiner.on(\", \").skipNulls();",
        "",
        "  private ArrayList<String> name = new ArrayList<String>();",
        "  private ArrayList<Integer> age = new ArrayList<Integer>();",
        "",
        "  /**",
        "   * Adds {@code element} to the list to be returned from {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code element} is null",
        "   */",
        "  public Person.Builder addName(String element) {",
        "    this.name.add(Preconditions.checkNotNull(element));",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Adds each element of {@code elements} to the list to be returned from",
        "   * {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code elements} is null or contains a",
        "   *     null element",
        "   */",
        "  public Person.Builder addName(String... elements) {",
        "    name.ensureCapacity(name.size() + elements.length);",
        "    for (String element : elements) {",
        "      addName(element);",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Adds each element of {@code elements} to the list to be returned from",
        "   * {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code elements} is null or contains a",
        "   *     null element",
        "   */",
        "  public Person.Builder addAllName(Iterable<? extends String> elements) {",
        "    if (elements instanceof Collection) {",
        "      name.ensureCapacity(name.size() + ((Collection<?>) elements).size());",
        "    }",
        "    for (String element : elements) {",
        "      addName(element);",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Clears the list to be returned from {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder clearName() {",
        "    this.name.clear();",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns an unmodifiable view of the list that will be returned by",
        "   * {@link Person#getName()}.",
        "   * Changes to this builder will be reflected in the view.",
        "   */",
        "  public List<String> getName() {",
        "    return Collections.unmodifiableList(name);",
        "  }",
        "",
        "  /**",
        "   * Adds {@code element} to the list to be returned from {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder addAge(int element) {",
        "    this.age.add(element);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Adds each element of {@code elements} to the list to be returned from",
        "   * {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder addAge(int... elements) {",
        "    age.ensureCapacity(age.size() + elements.length);",
        "    for (int element : elements) {",
        "      addAge(element);",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Adds each element of {@code elements} to the list to be returned from",
        "   * {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code elements} is null or contains a",
        "   *     null element",
        "   */",
        "  public Person.Builder addAllAge(Iterable<? extends Integer> elements) {",
        "    if (elements instanceof Collection) {",
        "      age.ensureCapacity(age.size() + ((Collection<?>) elements).size());",
        "    }",
        "    for (int element : elements) {",
        "      addAge(element);",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Clears the list to be returned from {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder clearAge() {",
        "    this.age.clear();",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns an unmodifiable view of the list that will be returned by",
        "   * {@link Person#getAge()}.",
        "   * Changes to this builder will be reflected in the view.",
        "   */",
        "  public List<Integer> getAge() {",
        "    return Collections.unmodifiableList(age);",
        "  }",
        "",
        "  private static final class Value extends Person {",
        "    private final List<String> name;",
        "    private final List<Integer> age;",
        "",
        "    private Value(Person_Builder builder) {",
        "      this.name = ImmutableList.copyOf(builder.name);",
        "      this.age = ImmutableList.copyOf(builder.age);",
        "    }",
        "",
        "    @Override",
        "    public List<String> getName() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public List<Integer> getAge() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Value)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Value other = (Person_Builder.Value) obj;",
        "      if (!name.equals(other.name)) {",
        "        return false;",
        "      }",
        "      if (!age.equals(other.age)) {",
        "        return false;",
        "      }",
        "      return true;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Arrays.hashCode(new Object[] { name, age });",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"Person{\"",
        "          + \"name=\" + name + \", \"",
        "          + \"age=\" + age + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created {@link Person} based on the contents of the {@code Builder}.",
        "   */",
        "  public Person build() {",
        "    return new Person_Builder.Value(this);",
        "  }",
        "",
        "  /**",
        "   * Sets all property values using the given {@code Person} as a template.",
        "   */",
        "  public Person.Builder mergeFrom(Person value) {",
        "    addAllName(value.getName());",
        "    addAllAge(value.getAge());",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Copies values from the given {@code Builder}.",
        "   */",
        "  public Person.Builder mergeFrom(Person.Builder template) {",
        "    addAllName(((Person_Builder) template).name);",
        "    addAllAge(((Person_Builder) template).age);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Resets the state of this builder.",
        "   */",
        "  public Person.Builder clear() {",
        "    name.clear();",
        "    age.clear();",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  private static final class Partial extends Person {",
        "    private final List<String> name;",
        "    private final List<Integer> age;",
        "",
        "    Partial(Person_Builder builder) {",
        "      this.name = ImmutableList.copyOf(builder.name);",
        "      this.age = ImmutableList.copyOf(builder.age);",
        "    }",
        "",
        "    @Override",
        "    public List<String> getName() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public List<Integer> getAge() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Partial)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Partial other = (Person_Builder.Partial) obj;",
        "      if (!name.equals(other.name)) {",
        "        return false;",
        "      }",
        "      if (!age.equals(other.age)) {",
        "        return false;",
        "      }",
        "      return true;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      int result = 1;",
        "      result *= 31;",
        "      result += ((name == null) ? 0 : name.hashCode());",
        "      result *= 31;",
        "      result += ((age == null) ? 0 : age.hashCode());",
        "      return result;",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"partial Person{\"",
        "          + COMMA_JOINER.join(",
        "              \"name=\" + name,",
        "              \"age=\" + age)",
        "          + \"}\";",
        "    }",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created partial {@link Person}",
        "   * based on the contents of the {@code Builder}.",
        "   * State checking will not be performed.",
        "   *",
        "   * <p>Partials should only ever be used in tests.",
        "   */",
        "  @VisibleForTesting()",
        "  public Person buildPartial() {",
        "    return new Person_Builder.Partial(this);",
        "  }",
        "}\n"));
  }

  private static Elements elements() {
    Enhancer e = new Enhancer();
    e.setClassLoader(ElementsImpl.class.getClassLoader());
    e.setSuperclass(ElementsImpl.class);
    CallbackHelper helper = new CallbackHelper(ElementsImpl.class, new Class<?>[] {}) {
      @Override
      protected Object getCallback(Method method) {
        if (Modifier.isAbstract(method.getModifiers())) {
          return new InvocationHandler() {
            @Override public Object invoke(Object proxy, Method method, Object[] args) {
              throw new UnsupportedOperationException(
                  "Not yet implemented by " + ElementsImpl.class.getCanonicalName());
            }
          };
        } else {
          return NoOp.INSTANCE;
        }
      }
    };
    e.setCallbacks(helper.getCallbacks());
    e.setCallbackFilter(helper);
    return (Elements) e.create();
  }

  abstract static class ElementsImpl implements Elements {
    @Override
    public Name getName(CharSequence cs) {
      return new NameImpl(cs.toString());
    }
  }

  private static GenericTypeElementImpl newTopLevelGenericType(String qualifiedName) {
    String pkg = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    PackageElement enclosingElement = new PackageElementImpl(pkg);
    return new GenericTypeElementImpl(enclosingElement, NoTypes.NONE, simpleName);
  }

  static class GenericTypeElementImpl extends ValueType
      implements javax.lang.model.element.TypeElement {

    private final Element enclosingElement;
    private final TypeMirror enclosingType;
    private final String simpleName;

    private GenericTypeElementImpl(
        Element enclosingElement, TypeMirror enclosingType, String simpleName) {
      this.enclosingElement = enclosingElement;
      this.enclosingType = enclosingType;
      this.simpleName = simpleName;
    }

    @Override
    protected void addFields(FieldReceiver fields) {
      fields.add("enclosingElement", enclosingElement);
      fields.add("enclosingType", enclosingType);
      fields.add("simpleName", simpleName);
    }

    public GenericTypeMirrorImpl newMirror(TypeMirror... typeArguments) {
      return new GenericTypeMirrorImpl(ImmutableList.copyOf(typeArguments));
    }

    @Override
    public TypeMirror asType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ElementKind getKind() {
      return ElementKind.CLASS;
    }

    @Override
    public Set<javax.lang.model.element.Modifier> getModifiers() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      return v.visitType(this, p);
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
      throw new UnsupportedOperationException();
    }

    @Override
    public NestingKind getNestingKind() {
      return (enclosingElement.getKind() == ElementKind.PACKAGE)
          ? NestingKind.TOP_LEVEL : NestingKind.MEMBER;
    }

    @Override
    public Name getQualifiedName() {
      return new NameImpl(GET_QUALIFIED_NAME.visit(enclosingElement) + "." + simpleName);
    }

    @Override
    public Name getSimpleName() {
      return new NameImpl(simpleName);
    }

    @Override
    public TypeMirror getSuperclass() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends TypeMirror> getInterfaces() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Element getEnclosingElement() {
      return enclosingElement;
    }

    // Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
      // TODO: handle Java 8 type annotations
      throw new UnsupportedOperationException();
    }

    // Override
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      // TODO: handle Java 8 type annotations
      throw new UnsupportedOperationException();
    }

    // Override
    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
      // TODO: handle Java 8 type annotations
      throw new UnsupportedOperationException();
    }

    class GenericTypeMirrorImpl extends ValueType implements DeclaredType {
      private final ImmutableList<TypeMirror> typeArguments;

      GenericTypeMirrorImpl(ImmutableList<TypeMirror> typeArguments) {
        this.typeArguments = typeArguments;
      }

      @Override
      protected void addFields(FieldReceiver fields) {
        fields.add("GenericTypeElementImpl.this", GenericTypeElementImpl.this); 
        fields.add("typeArguments", typeArguments);
      }

      @Override
      public TypeKind getKind() {
        return TypeKind.DECLARED;
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitDeclared(this, p);
      }

      @Override
      public GenericTypeElementImpl asElement() {
        return GenericTypeElementImpl.this;
      }

      @Override
      public TypeMirror getEnclosingType() {
        return enclosingType;
      }

      @Override
      public ImmutableList<TypeMirror> getTypeArguments() {
        return typeArguments;
      }
    }
  }

  private static final AbstractElementVisitor6<Name, ?> GET_QUALIFIED_NAME =
      new AbstractElementVisitor6<Name, Void>() {

        @Override
        public Name visitPackage(PackageElement e, Void p) {
          return e.getQualifiedName();
        }

        @Override
        public Name visitType(TypeElement e, Void p) {
          return e.getQualifiedName();
        }

        @Override
        public Name visitVariable(VariableElement e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public Name visitExecutable(ExecutableElement e, Void p) {
          throw new IllegalArgumentException();
        }

        @Override
        public Name visitTypeParameter(TypeParameterElement e, Void p) {
          throw new IllegalArgumentException();
        }
      };
}

