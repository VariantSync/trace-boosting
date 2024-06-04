# trace-boosting: Give an Inch and Take a Mile? Effects of Adding Reliable Knowledge to Heuristic Feature Tracing

## Overview
This research artifact is based on the findings of our SPLC 2024 paper on boosting retroactive feature tracing techniques with proactive traces. 
Our paper explores how providing a minimal seed of accurate feature traces proactively can significantly enhance the accuracy of automated, heuristic-based retroactive tracing. 
The paper demonstrates that increasing amounts of proactive information can boost the overall accuracy of the tracing and that the number of variants compared affects the effectiveness of the combined tracing approach.

TraceBoosting is an algorithm designed to enhance retroactive feature tracing with proactively collected feature traces. 
It is particularly useful for projects with multiple product variants, where it can improve the accuracy and efficiency of the tracing process. 
This projects presents a prototype of our algorithm that boosts comparison-based retroactive feature tracing. 
The used retroactive tracing method is heavily inspired by ECCO.

## Setup Using Maven
To include the TraceBoosting library in your Maven project, add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>org.variantsync</groupId>
    <artifactId>traceboosting</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage
To use the TraceBoosting algorithm, follow these steps:

1. Initialize a list to hold `ProductPassport` objects that describe the artifact locations for each variant.
2. Iterate over the collection of variants for which traces are to be computed, creating a `ProductPassport` for each and adding it to the list.
3. Instantiate the TraceBoosting algorithm with the product passports, working directory, and the supported language for tracing.
4. Retrieve the list of products from the TraceBoosting instance.
5. Apply the proactively collected traces to the products.
6. Compute the Main tree, which represents the merged variant AST with feature traces.

Here is a code snippet demonstrating how to use the TraceBoosting algorithm:

```java
List<ProductPassport> productPassports = new ArrayList<>();
for (Variant variant : variants) {
    String variantName = variant.getName();
    productPassports.add(new ProductPassport(variantName,
            variantsDirectory.resolve(variantName), configFileMap.get(variantName)));
}

TraceBoosting traceBoosting = new TraceBoosting(productPassports,
        workingDirectory, ESupportedLanguages.LINES);

List<Product> products = traceBoosting.getProducts();

// Now apply proactively created traces to the products. You can directly access the AST nodes of the products.
// TODO: Implement distribution of proactively collected mappings

// Finally, execute the boosting algorithm
MainTree mainTree = traceBoosting.compute();
```
