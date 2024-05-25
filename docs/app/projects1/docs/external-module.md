# External Module

## 

First, You must create or have an output port in the application service layer. So, You create an adapter for implement
these ports.

```mermaid
flowchart LR
   subgraph application service
       output_port
   end
   subgraph external module
     secondary_adapter
   end
    output_port --> secondary_adapter
```

Part I.

- Dto from Client Specific(e.g: Rest, Soap, gRpc, GraphQL, etc)
- Mappers

Part II.

- Implementing output ports(Publishers/Producers)
    - Secondary Adapter