# Hybris / ERP integration demo

Piccolo progetto didattico in Java 17 e Spring Boot 3, senza librerie SAP. Simula due microservizi separati e include una GUI web per seguire visivamente le chiamate:

- `erp-mock`, porta `8081`: ERP minimale con prezzi, ATP e ricezione ordini in memoria.
- `commerce-mock`, porta `8080`: storefront/Commerce con Product e Order su H2 in-memory.
- `demo-ui`, porta `3000`: dashboard interattiva per eseguire le operazioni e visualizzare il flusso.

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

Aprire la GUI in [http://localhost:3000](http://localhost:3000). Dalla dashboard puoi eseguire le tre scene con i pulsanti **Import into Commerce**, **Search** e **Create order**. Il pannello **Scene trace** mostra la sequenza Browser → Commerce/H2 → ERP → risposta, con endpoint e payload sintetici.

Per fermare i container:

```powershell
docker compose down
```

## Flusso end-to-end

### Percorso visuale consigliato

1. Avvia lo stack e apri [http://localhost:3000](http://localhost:3000).
2. In **ImpEx import**, seleziona `products.csv` e premi **Import into Commerce**. La scena mostra l'upsert nel catalogo locale H2.
3. In **Product search**, lascia `laptop` e premi **Search**. I risultati locali vengono arricchiti con prezzo e ATP live dell'ERP.
4. In **Checkout handoff**, usa `SKU-100`, quantità `2`, e premi **Create order**. La trace rende visibili `CREATED`, `POST /erp/orders` e `CONFIRMED`.
5. Il box **Payload / result** mostra la risposta JSON finale e il pulsante **Reset** azzera la scena.

La trace è una spiegazione visuale del percorso applicativo osservato dalla GUI, non un sistema di distributed tracing reale.

### Percorso API con curl

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
| `demo-ui/index.html`, `app.js`, `styles.css` | GUI didattica: scene operative, timeline e risultati |
| `demo-ui/nginx.conf` | Static server e reverse proxy dalla GUI verso Commerce |
| `docker-compose.yml` | Topologia locale dei due sistemi e configurazione endpoint ERP |
