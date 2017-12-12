---
layout: default
title: Documentation
permalink: /documentation/
---

## Summary

HyperGraphQL is a [GraphQL](http://graphql.org) interface for querying and serving [linked data](https://www.w3.org/standards/semanticweb/data) on the Web. It is designed to support federated querying and exposing data from multiple linked data services using GraphQL query language and schemas. 

HyperGraphQL serves several key objectives:

- hiding the complexities of the Semantic Web stack behind the GraphQL interface, thus enabling access to linked data via a simpler and more familiar to many clients GraphQL interface;
- enforcing a unified view on the data residing in disconnected multiple services, while preserving the original URIs and links between them; 
- restricting access to RDF stores (as one of supported services) down to naturally definable subsets of (tree-shaped) queries, which can be more efficiently handled, thus minimising the impact on the stores' availability;
- enabling easy deployment of reasonably sized linked datasets via embedded, in-memory [Apache Jena](https://jena.apache.org/) storage, and exposing them via GraphQL endpoint and accompanying [GraphiQL](https://github.com/graphql/graphiql) UI.

The basic response fromat is [JSON-LD](json-ld.org), which extends the standard JSON with the [JSON-LD context](https://json-ld.org/spec/latest/json-ld-api-best-practices/#dfn-json-ld-context) enabling semantic disambiguation of the contained data.

## GraphQL schema + RDF mapping + service configs = HyperGraphQL server

To set up a HyperGraphQL server you only need to provide your GraphQL type schema and its mapping to the target RDF vocabulary and SPARQL endpoints. The complete GraphQL wiring is conducted automatically on initiating the server. 

![HyperGraphQL-screenshot](../sources/screenshot.png)

## Running

Clone the Git repository into a local directory. Then in the root of the project execute the following: 

**Maven**: 
1. **mvn install**
2. **mvn exec:java**


(*Note*: in Windows these must be executed in a *cmd* terminal, not *PowerShell*).

**Gradle**: 
1. **gradle build**
2. **gradle execute**


By deafault, the HyperGraphQL server starts at: 

```
http://localhost:8080/graphql
```

The [GraphiQL](https://github.com/graphql/graphiql) UI is initiated at:

```
http://localhost:8080/graphiql
```


## Properties

Basic settings are defined in the *properties.json* file. The defaults are:

```js
{
    "schemaFile": "schema.graphql",
    "contextFile": "context.json",
    "graphql": {
        "port": 8080,
        "path": "/graphql",
        "graphiql": "/graphiql"
    }
}
```

- *schemaFile*: the file containing GraphQL schema definition;
- *contextFile*: the file containing mapping from the schema file to RDF vocabularies and respective SPARQL endpoints to be used for resolving GraphQL fields;
- *graphql.port*: the port at thich the GraphQL server and GraphiQL interface are initiated;
- *graphql.path*: the URL path of the GraphQL server;
- *graphql.graphiql*: the URL path of the GraphiQL UI.

## Example

The following query requests a single person instance with its URI (*_id*) and RDF type (*_type*), its name, birth date; further its birth place with the URI, an english label, and the country in which it is located, also including its URI and an english label. 

### HyperGraphQL query:
```
{
  people(limit: 1, offset: 6) {
    _id
    _type
    name
    birthDate
    birthPlace {
      _id
      label(lang: "en")
      country {
        _id
        label(lang: "en")
      }
    }
  }
}
```

The response of HyperGraphQL server to this query consists of the usual GraphQL JSON object, further augmented with a JSON-LD context, included as the value of the property "@context" on the "data" object.

### HyperGraphQL response:
```json
{
  "extensions": {},
  "data": {
    "people": [
      {
        "_id": "http://dbpedia.org/resource/Sani_ol_molk",
        "_type": "http://dbpedia.org/ontology/Person",
        "name": "Mirza Abolhassan Khan Ghaffari",
        "birthDate": "1814-1-1",
        "birthPlace": {
          "_id": "http://dbpedia.org/resource/Kashan",
          "label": [
            "Kashan"
          ],
          "country": {
            "_id": "http://dbpedia.org/resource/Iran",
            "label": [
              "Iran"
            ]
          }
        }
      }
    ],
    "@context": {
      "birthPlace": "http://dbpedia.org/ontology/birthPlace",
      "country": "http://dbpedia.org/ontology/country",
      "_type": "@type",
      "name": "http://xmlns.com/foaf/0.1/name",
      "_id": "@id",
      "label": "http://www.w3.org/2000/01/rdf-schema#label",
      "people": "http://hypergraphql/query/people",
      "birthDate": "http://dbpedia.org/ontology/birthDate"
    }
  },
  "errors": []
}
```

It's easy to find out, using e.g. [JSON-LD playground](https://json-ld.org/playground/), that the "data" element in this response is in fact a valid JSON-LD object encoding the following RDF graph (in N-TRIPLE notation):

```
_:b0 <http://hypergraphql/query/people> <http://dbpedia.org/resource/Sani_ol_molk> .
<http://dbpedia.org/resource/Sani_ol_molk> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Person> .
<http://dbpedia.org/resource/Sani_ol_molk> <http://xmlns.com/foaf/0.1/name> "Mirza Abolhassan Khan Ghaffari" .
<http://dbpedia.org/resource/Sani_ol_molk> <http://dbpedia.org/ontology/birthDate> "1814-1-1" .
<http://dbpedia.org/resource/Sani_ol_molk> <http://dbpedia.org/ontology/birthPlace> <http://dbpedia.org/resource/Kashan> .
<http://dbpedia.org/resource/Kashan> <http://www.w3.org/2000/01/rdf-schema#label> "Kashan" .
<http://dbpedia.org/resource/Kashan> <http://dbpedia.org/ontology/country> <http://dbpedia.org/resource/Iran> .
<http://dbpedia.org/resource/Iran> <http://www.w3.org/2000/01/rdf-schema#label> "Iran" .
```
This graph (except for the first triple, added by the HyperGraphQL server) is a subset of the DBpedia dataset. 

## Schema

The schema definition complies with the GraphQL spec (see: 	[http://graphql.org/learn/schema/](http://graphql.org/learn/schema/)). Currently, only the core fragment of the spec, including object types and fields, is supported, as presented in the example below. Additionally, some default SPARQL-related fields and arguments are added automatically to each schema. 


```
type Query {
    people: [Person]
    cities: [City]
}

type Person {
    name: String
    label: [String]
    birthPlace: City
    birthDate: String
}

type City {
    label: [String]
    country: Country
    leader: Person
}

type Country {
    label: [String]
}
```

## RDF/service mapping

The RDF/service mapping consists of two components:

- *predicates*: defining the URIs associated with the GraphQL vocabulary and the services from which the data is to be fetched;
- *services*: providing the details of each involved service (SPARQL endpoint).

The following example presents a possible mapping for the schema above, where all predicates are associated with the default graph of *http://dbpedia.org* SPARQL endpoint.

```json
{
  "predicates": {
    "queryFields": {
      "people": {
        "service": "dbpedia"
      },
      "cities": {
        "service": "dbpedia"
      }
    },
    "types": {
      "Person": {
        "@id": "http://dbpedia.org/ontology/Person"
      },
      "City": {
        "@id": "http://dbpedia.org/ontology/City"
      }
    },
    "fields": {
      "name": {
        "@id": "http://xmlns.com/foaf/0.1/name",
        "service": "dbpedia"
      },
      "birthDate": {
        "@id": "http://dbpedia.org/ontology/birthDate",
        "service": "dbpedia"
      },
      "birthPlace": {
        "@id": "http://dbpedia.org/ontology/birthPlace",
        "service": "dbpedia"
      },
      "label": {
        "@id": "http://www.w3.org/2000/01/rdf-schema#label",
        "service": "dbpedia"
      },
      "country": {
        "@id": "http://dbpedia.org/ontology/country",
        "service": "dbpedia"
      },
      "leader": {
        "@id": "http://dbpedia.org/ontology/leaderName",
        "service": "dbpedia"
      }
    }
  },
  "services": [
    {
      "@id": "dbpedia",
      "@type": "SparqlEndpoint",
      "url": "http://dbpedia.org/sparql/",
      "graph": "http://dbpedia.org",
      "user": "",
      "password": ""
    }
  ]
}
```

Note that: 
1) query fields (here: *people* and *cities*) are not mapped to URIs, but must be associated with some service;
2) object types, at least those that serve as output types of the query fields (here *Person* and *City*), must be associated with URIs, but not with the named graphs, as they will always be associated with the endpoint of the field;
3) each (non-query) field must be associated with a URI and a service;
4) each service must be specified with the URL of the SPARQL endpoint, the graph (id of a named graph), and authentication details. Whenever authentication is not required, the user and password are asserted as empty strings.

HyperGraphQL supports also federated querying over a collection of SPARQL endpoints, although the current prototype implementation requires further optimizations. The federation is achieved by associating predicates with different services.  

## Query rewriting 

GraphQL queries are rewritten into SPARQL construct queries and executed against the designated SPARQL endpoints. 

All fields of the **Query** type are rewritten into instance subqueries, for intance:

```
{
people (limit:1, offset:6) {
  ...
}
}
```
is rewritten into:
```
{
  SELECT ?subject 
  WHERE {
    ?subject a <http://dbpedia.org/ontology/Person> .
  } LIMIT 1 OFFSET 6
}
...
```

Fields are translated into optional SPARQL triple patterns, additionally filtered with the RDF type associated with the output type of the field, provided such URI is specified in the mapping, for instance:
```
{
... {
  birthplace 
  ...
}
}
```
is rewritten:

```
...
OPTIONAL {
    ?subject <http://dbpedia.org/ontology/birthPlace> ?object .
    ?object a <http://dbpedia.org/ontology/City> .
    ...
}
```

## Execution

To minimise the number of return trips between HyperGraphQL server and RDF stores, the original GraphQL query is translated into possibly few SPARQL CONSTRUCT queries necessary to fetch all the relevant RDF data. The further transformation of the data into  HyperGraphQL responses is done locally by the HyperGraphQL server. When the query requests data from a single SPARQL endpoint, only one SPARQL CONSTRUCT query is issued. 


## Other references

* S. Klarman, [Querying DBpedia with GraphQL](https://medium.com/@sklarman/querying-linked-data-with-graphql-959e28aa8013), blog post, Semantic Integration, 2017.