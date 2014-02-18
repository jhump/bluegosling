/**
 * APIs for tagged union types. These types are also known as disjoint unions, discriminating
 * unions, sum types, and variant types. They provide a type-safe and efficient way to implement
 * this pattern in Java. Included are union types with between two and five (inclusive) components,
 * of which only one may be present at any time. They are divided into two families of
 * implementations:
 * <ol>
 * <li><strong>Either / Any</strong>: These types require that exactly one component be present and
 *    non-null.</li>
 * <li><strong>Variant</strong>: These types require that exactly one component be present, but
 *    null components are allowed. They are more like traditional union or variant types in that
 *    they do not place constraints on the values that may be present.</li>
 * </ol>
 * All of the types herein extend from the main interface {@link com.apriori.choice.Choice}.
 */
package com.apriori.choice;