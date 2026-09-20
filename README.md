# Hybris / ERP integration demo

Piccolo progetto didattico in Java 17 e Spring Boot 3, senza librerie SAP. Simula due microservizi separati:

- `erp-mock`, porta `8081`: ERP minimale con prezzi, ATP e ricezione ordini in memoria.
- `commerce-mock`, porta `8080`: storefront/Commerce con Product e Order su H2 in-memory.

## Prerequisiti

- Java 17
- Maven 3.9+
- In alternativa, Docker Desktop con Docker Compose

## Avvio locale con Maven

Aprire due terminali dalla root del progetto:

```powershell
cd erp-mock
mvn spring-boot:run
```

```powershell
cd commerce-mock
mvn spring-boot:run
```

`commerce-mock` usa `http://localhost:8081` come ERP di default. Per cambiare URL:

```powershell
$env:ERP_BASE_URL="http://nome-host:8081"
mvn spring-boot:run
```

## Avvio con Docker Compose

Dalla root:

```powershell
docker compose up --build
```

In Compose, `commerce-mock` raggiunge l'ERP tramite `http://erp-mock:8081`; dall'host le API restano disponibili su `localhost:8080` e `localhost:8081`.

Per fermare i container:

```powershell
docker compose down
```

## Flusso end-to-end

1. Import CSV, con upsert per `code`:

```powershell
curl.exe -X POST http://localhost:8080/impex/import -F "file=@products.csv"
```

Risposta attesa: `{"importedProducts":3}`.

2. Ricerca prodotto: i dati anagrafici arrivano da H2 e `price`/`availableToPromise` arrivano in tempo reale dall'ERP:

```powershell
curl.exe "http://localhost:8080/occ/v2/products/search?query=laptop"
```

3. Creazione ordine locale e invio all'ERP:

```powershell
curl.exe -X POST http://localhost:8080/occ/v2/orders -H "Content-Type: application/json" -d "{`"productCode`":`"SKU-100`",`"quantity`":2}"
```

La risposta contiene l'ID locale, `status: "CONFIRMED"` e l'`erpOrderId` generato dall'ERP.

4. Verifica dello stato persistito localmente, sostituendo `1` con l'ID restituito:

```powershell
curl.exe http://localhost:8080/occ/v2/orders/1
```

5. Verifica diretta dei dati ERP (facoltativa):

```powershell
curl.exe http://localhost:8081/erp/products/SKU-100/stock
```

## Struttura principale

```text
.
|-- docker-compose.yml
|-- products.csv
|-- erp-mock/
|   |-- pom.xml
|   |-- Dockerfile
|   `-- src/main/java/demo/hybris/erp/
`-- commerce-mock/
    |-- pom.xml
    |-- Dockerfile
    `-- src/main/java/demo/hybris/commerce/
```

Nota didattica: il parser CSV è intenzionalmente minimale e si aspetta quattro colonne senza virgole nei valori. In un progetto reale si userebbe una libreria CSV e una gestione errori/retry per le chiamate ERP.

## Mappa concetti

| Codice | Concetto SAP Commerce / ERP rappresentato |
|---|---|
| `commerce-mock/Product.java` | Type System: tipo prodotto locale con attributi di catalogo |
| `commerce-mock/ProductRepository.java` | Persistenza del catalogo e query custom in stile FlexibleSearch |
| `commerce-mock/ImpexController.java` | ImpEx: import bulk CSV con upsert per codice |
| `commerce-mock/CommerceController.java` | OCC API: endpoint REST per ricerca prodotto e ordine |
| `commerce-mock/ErpClient.java` | Integrazione ERP outbound tramite `RestClient` |
| `commerce-mock/Order.java` | Modello ordine locale e stato del processo Commerce |
| `erp-mock/ErpController.java` | API ERP per prezzo, ATP e conferma ordine |
| `docker-compose.yml` | Topologia locale dei due sistemi e configurazione endpoint ERP |
