# OO Design Principles Catalog

This reference contains the full principle definitions, anti-patterns, and corrective patterns extracted from the book. The agent should consult this file when it needs detailed examples or precise definitions during a review.

---

## P1 — Cohesion and Single Responsibility Principle (SRP)

**Core rule:** A class should have only one reason to change. It must represent a single, well-defined concept.

**Violation signals:**
- A class contains `if/else` or `when` chains that branch on a type, role, or category to execute different business rules.
- A class grows indefinitely as new variants are added.
- A class name uses generic words like "Manager", "Handler", "Processor" combined with multiple unrelated behaviors.

**Corrective pattern:** Extract each varying rule into its own small class that implements a common interface. The original class delegates to the appropriate implementation.

**Bad example — branching on role:**
```java
class CalculadoraDeSalario {
    public double calcula(Funcionario funcionario) {
        if(DESENVOLVEDOR.equals(funcionario.getCargo())) {
            return funcionario.getSalarioBase() * 0.9;
        }
        if(DBA.equals(funcionario.getCargo())) {
            return funcionario.getSalarioBase() * 0.85;
        }
        throw new RuntimeException("funcionario invalido");
    }
}
```

**Good example — strategy per rule:**
```java
public interface RegraDeCalculo {
    double calcula(Funcionario f);
}

public class DezOuVintePorCento implements RegraDeCalculo {
    public double calcula(Funcionario funcionario) {
        if(funcionario.getSalarioBase() > 3000.0) return funcionario.getSalarioBase() * 0.8;
        return funcionario.getSalarioBase() * 0.9;
    }
}
```

---

## P2 — Coupling and Dependency Inversion Principle (DIP)

**Core rule:** Couple to stable abstractions, not to concrete implementations. High-level workflow classes must not depend on low-level details.

**Violation signals:**
- Constructor or method directly instantiates a concrete dependency (e.g., `new EmailSender()`).
- A class imports multiple concrete infrastructure classes (database, HTTP client, file system).
- Changing a low-level component forces changes in a high-level orchestrator.

**Corrective pattern:** Introduce an abstraction (interface) for each external dependency. Inject through the constructor. Use patterns like Observer to decouple actions from orchestrators.

**Bad example — concrete coupling:**
```java
public class GeradorDeNotaFiscal {
    private final EnviadorDeEmail email;
    private final NotaFiscalDao dao;

    public GeradorDeNotaFiscal(EnviadorDeEmail email, NotaFiscalDao dao) {
        this.email = email;
        this.dao = dao;
    }
}
```

**Good example — abstraction-based coupling:**
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

**Core rule:** Classes should be open for extension but closed for modification. Behavior changes should be achievable by injecting different implementations, not by editing source code.

**Violation signals:**
- A class instantiates its own dependencies internally with `new`.
- Adding a new behavior variant requires modifying the existing class.
- The class cannot be tested in isolation because it hardcodes collaborators.

**Corrective pattern:** Accept dependencies via constructor injection using interfaces. New behaviors are added by creating new implementations, not modifying existing code.

**Bad example — hardcoded dependencies:**
```java
public class CalculadoraDePrecos {
    public double calcula(Compra produto) {
        TabelaDePrecoPadrao tabela = new TabelaDePrecoPadrao();
        Frete correios = new Frete();
        double desconto = tabela.descontoPara(produto.getValor());
        double frete = correios.para(produto.getCidade());
        return produto.getValor() * (1-desconto) + frete;
    }
}
```

**Good example — injected dependencies:**
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

## P4 — Encapsulation and Change Propagation

**Core rule:** Hide how a class performs its tasks. Follow "Tell, Don't Ask": do not extract state from an object to make decisions externally — tell the object what to do and let it handle its own logic.

**Violation signals:**
- Code calls multiple getters on an object, performs logic, then calls setters to update it.
- Business rules that belong to an entity are scattered across external service or manager classes.
- Changing an internal rule requires finding and updating multiple external callers.

**Corrective pattern:** Move the decision-making logic into the owning class. The external caller should invoke a single meaningful method instead of orchestrating state changes.

**Bad example — Tell, Don't Ask violation:**
```java
public class ProcessadorDeBoletos {
    public void processa(List<Boleto> boletos, Fatura fatura) {
        double total = 0;
        for(Boleto boleto : boletos) {
            total += boleto.getValor();
        }
        if(total >= fatura.getValor()) {
            fatura.setPago(true);
        }
    }
}
```

**Good example — encapsulated state management:**
```java
public class Fatura {
    public void adicionaPagamento(Pagamento pagamento) {
        this.pagamentos.add(pagamento);
        if(valorTotalDosPagamentos() >= this.valor) {
            this.pago = true;
        }
    }
}
```

---

## P5 — Inheritance vs Composition and Liskov Substitution Principle (LSP)

**Core rule:** Favor composition over inheritance unless a strict "is-a" relationship exists. If inheritance is used, child classes must never break the parent's contract (no unexpected exceptions, no weakened postconditions).

**Violation signals:**
- A subclass throws `UnsupportedOperationException` or similar for inherited methods.
- Inheritance is used purely to reuse code, not to express a type hierarchy.
- Substituting a child for its parent changes observable behavior in a breaking way.

**Corrective pattern:** Extract shared behavior into a helper class and compose it into each class that needs it. Each class only exposes the methods it truly supports.

**Bad example — LSP violation:**
```java
public class ContaComum {
    public void rende() { this.saldo *= 1.1; }
}

public class ContaDeEstudante extends ContaComum {
    public void rende() {
        throw new ContaNaoRendeException();
    }
}
```

**Good example — composition instead of inheritance:**
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
    // Does not expose rende() at all
}
```

---

## P6 — Thin Interfaces and Interface Segregation Principle (ISP)

**Core rule:** Interfaces must be cohesive and thin. Never force a class to implement methods it cannot logically support. Method parameters should demand the thinnest possible type.

**Violation signals:**
- A class implements an interface method by throwing an exception or returning a dummy value.
- An interface mixes unrelated responsibilities (e.g., calculation + document generation).
- A method receives a large object but only uses one or two of its fields.

**Corrective pattern:** Split fat interfaces into smaller, cohesive contracts. Each class implements only the interfaces whose methods it truly supports. Use narrow parameter types.

**Bad example — fat interface:**
```java
interface Imposto {
    NotaFiscal geraNota();
    double imposto(double valorCheio);
}

class IXMX implements Imposto {
    public double imposto(double valorCheio) { return 0.2 * valorCheio; }
    public NotaFiscal geraNota() { throw new NaoGeraNotaException(); }
}
```

**Good example — segregated interfaces:**
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

## P7 — Consistency, Tiny Types, and Rich Constructors

**Core rule:** Objects must never exist in an invalid state. Mandate all required attributes at instantiation via rich constructors. Avoid empty constructors followed by setter chains.

**Violation signals:**
- Classes have no-arg constructors with mandatory fields set via setters after creation.
- Objects are passed around in partially initialized states.
- Null checks for fields that should always be present are scattered throughout the codebase.

**Corrective pattern:** Use constructors that require all mandatory fields. Validate invariants at construction time. Consider tiny types (value objects) for domain-specific primitives.

**Bad example — incomplete object:**
```java
Pedido festa = new Pedido();
festa.adicionaItem(new Item("SALGADO", 50.0));
// Missing Cliente — will crash later
```

**Good example — rich constructor:**
```java
class Pedido {
    public Pedido(Cliente cliente) {
        this.cliente = cliente;
        this.valorTotal = 0;
        this.itens = new ArrayList<Item>();
    }
}
Pedido festa = new Pedido(new Cliente("Mauricio"));
```

---

## P8 — Design Smells (Feature Envy)

**Core rule:** A method should primarily use data and behavior from its own class. "Feature Envy" occurs when a method is more interested in the data of another object than its own. This treats objects as anemic data structures and scatters logic.

**Violation signals:**
- A method calls multiple getters on another object, performs calculations, then calls setters on it.
- A class has methods that mostly operate on another class's fields.
- Business logic that naturally belongs to an entity is placed in a service or manager class.

**Corrective pattern:** Push the envied logic into the class that owns the data. The external caller should invoke a single method that encapsulates the full behavior.

**Bad example — Feature Envy:**
```java
class Gerenciador {
    public void processa(NotaFiscal nf) {
        double imposto = nf.calculaImposto();
        if(nf.getQtdDeItens() > 2) { imposto = imposto * 1.1; }
        nf.setaValorImposto(imposto);
        nf.finaliza();
    }
}
```

**Good example — behavior in the owning class:**
```java
class NotaFiscal {
    public void processa() {
        double imposto = calculaImposto();
        if(this.qtdDeItens > 2) { imposto = imposto * 1.1; }
        this.valorImposto = imposto;
        this.finalizada = true;
    }
}
// Gerenciador now just calls nf.processa();
```
