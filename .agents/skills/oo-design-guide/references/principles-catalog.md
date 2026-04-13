# OO Design Principles — Builder's Reference

This reference contains the 8 design principles the agent must apply when guiding OO design decisions. Each principle includes the core rule, design questions to ask, and the pattern to follow.

---

## P1 — Cohesion and Single Responsibility Principle (SRP)

**Core rule:** A class must have only one reason to change. It represents a single, well-defined concept.

**Design questions to ask:**
- What is the single responsibility of this class?
- If I described what this class does, would I use the word "and"? (If yes, split it.)
- Will adding a new variant (e.g., a new role, type, or category) require modifying this class?

**Pattern to follow:** When multiple variants of a behavior exist, define a common interface and implement each variant in its own small class. The orchestrator delegates to the right implementation.

```java
// Define a contract for the varying behavior
public interface RegraDeCalculo {
    double calcula(Funcionario f);
}

// Each variant is its own cohesive class
public class DezOuVintePorCento implements RegraDeCalculo {
    public double calcula(Funcionario funcionario) {
        if(funcionario.getSalarioBase() > 3000.0) return funcionario.getSalarioBase() * 0.8;
        return funcionario.getSalarioBase() * 0.9;
    }
}
```

---

## P2 — Coupling and Dependency Inversion Principle (DIP)

**Core rule:** Depend on stable abstractions, never on concrete implementations. High-level classes must not know about low-level details.

**Design questions to ask:**
- Does this class know the concrete type of its collaborators?
- If I swap the database, email provider, or external service, would this class need to change?
- Can I test this class without spinning up real infrastructure?

**Pattern to follow:** Define an interface for every external or infrastructure dependency. Accept it through the constructor. If a class triggers multiple side effects after an action, use the Observer pattern with an action interface.

```java
interface AcaoAposGerarNota {
    void executa(NotaFiscal nf);
}

public class GeradorDeNotaFiscal {
    private final List<AcaoAposGerarNota> acoes;

    public GeradorDeNotaFiscal(List<AcaoAposGerarNota> acoes) {
        this.acoes = acoes;
    }
}
```

---

## P3 — Open-Closed Principle (OCP)

**Core rule:** Design classes so that behavior can be extended without modifying existing source code. Achieve this through constructor injection of interface-typed dependencies.

**Design questions to ask:**
- Can I change what this class does by passing in a different implementation, without editing it?
- Does this class instantiate its own collaborators with `new`?
- When a new business requirement arrives, will I need to open this file?

**Pattern to follow:** Accept all behavioral dependencies via the constructor using interface types. New behaviors are added by creating new implementations and wiring them at composition time.

```java
public class CalculadoraDePrecos {
    private TabelaDePreco tabela;
    private ServicoDeEntrega entrega;

    public CalculadoraDePrecos(TabelaDePreco tabela, ServicoDeEntrega entrega) {
        this.tabela = tabela;
        this.entrega = entrega;
    }

    public double calcula(Compra produto) {
        double desconto = tabela.descontoPara(produto.getValor());
        double frete = entrega.para(produto.getCidade());
        return produto.getValor() * (1-desconto) + frete;
    }
}
```

---

## P4 — Encapsulation and Tell Don't Ask

**Core rule:** Objects hide how they work. Never extract an object's state to make decisions externally — tell the object what to do and let it manage its own logic.

**Design questions to ask:**
- Am I calling getters on this object to make a decision that the object itself should make?
- If this business rule changes, how many places in the codebase would I need to update?
- Does this method belong to the class it is in, or to the object it is manipulating?

**Pattern to follow:** Place decision-making logic inside the class that owns the data. Expose meaningful action methods instead of raw state.

```java
public class Fatura {
    public void adicionaPagamento(Pagamento pagamento) {
        this.pagamentos.add(pagamento);
        if(valorTotalDosPagamentos() >= this.valor) {
            this.pago = true;
        }
    }
}
// External code just calls: fatura.adicionaPagamento(pagamento)
```

---

## P5 — Composition over Inheritance and Liskov Substitution (LSP)

**Core rule:** Prefer composition over inheritance. Use inheritance only for strict "is-a" relationships. If inheritance is used, the child must never break the parent's contract.

**Design questions to ask:**
- Is this really an "is-a" relationship, or am I just reusing code?
- Can every subclass fulfill all of the parent's methods without throwing exceptions or returning dummy values?
- Would composition (delegating to a helper) be simpler and more flexible?

**Pattern to follow:** Extract shared behavior into a helper class. Compose it into each class that needs it. Each class only exposes the methods it truly supports.

```java
class ManipuladorDeSaldo {
    public void juros(double taxa) { ... }
}

class ContaComum {
    private ManipuladorDeSaldo manipulador = new ManipuladorDeSaldo();
    public void rende() { manipulador.juros(0.1); }
}

class ContaDeEstudante {
    private ManipuladorDeSaldo manipulador = new ManipuladorDeSaldo();
    // Does not expose rende() — no fake contract
}
```

---

## P6 — Thin Interfaces and Interface Segregation (ISP)

**Core rule:** Interfaces must be cohesive and thin. Never force a class to implement methods it does not logically support. Method parameters should accept the narrowest possible type.

**Design questions to ask:**
- Does every class that implements this interface use all of its methods?
- Would any implementer need to throw UnsupportedOperationException for a method?
- Am I passing a large object when the method only needs one or two fields?

**Pattern to follow:** Split fat interfaces into smaller, focused contracts. Each class implements only the interfaces it truly fulfills. Use narrow parameter types.

```java
interface CalculadorDeImposto {
    double imposto(double valorCheio);
}

interface GeradorDeNota {
    NotaFiscal geraNota();
}

class ISS implements CalculadorDeImposto, GeradorDeNota { ... }
class IXMX implements CalculadorDeImposto { ... }
```

---

## P7 — Rich Constructors, Tiny Types, and Consistency

**Core rule:** Objects must never exist in an invalid state. Mandate all required attributes at instantiation. Validate invariants in the constructor. Consider tiny types (value objects) for domain primitives.

**Design questions to ask:**
- Can someone create an instance of this class without providing all required data?
- Are there setter calls that could be forgotten, leaving the object incomplete?
- Are primitive types being used where a domain-specific value object would add clarity and validation?

**Pattern to follow:** Require all mandatory fields in the constructor. Remove no-arg constructors when they allow invalid state. Use tiny types for domain primitives like Email, Money, or CustomerId.

```java
class Pedido {
    public Pedido(Cliente cliente) {
        this.cliente = cliente;
        this.valorTotal = 0;
        this.itens = new ArrayList<Item>();
    }
}
// Impossible to create a Pedido without a Cliente
Pedido festa = new Pedido(new Cliente("Mauricio"));
```

---

## P8 — Avoiding Design Smells (Feature Envy)

**Core rule:** A method should primarily use data from its own class. When a method is more interested in another object's data than its own, the logic is in the wrong place.

**Design questions to ask:**
- Is this method calling multiple getters on another object to compute a result?
- Does this logic belong to the class it is in, or to the object whose data it uses?
- If I moved this method to the other class, would it be simpler and more cohesive?

**Pattern to follow:** Move the logic into the class that owns the data. The external caller invokes a single method that encapsulates the full behavior.

```java
class NotaFiscal {
    public void processa() {
        double imposto = calculaImposto();
        if(this.qtdDeItens > 2) { imposto = imposto * 1.1; }
        this.valorImposto = imposto;
        this.finalizada = true;
    }
}
// External code just calls: nf.processa()
```
