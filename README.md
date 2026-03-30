# ClearLag

Een eenvoudige ClearLag-plugin voor Paper servers. Deze plugin verwijdert automatisch items die op de grond liggen en kan vooraf waarschuwingen sturen naar alle spelers.

## Functies

- Automatische cleanup van grond-items
- Instelbare eerste delay en herhaal-interval
- Aankondigingen vlak voor de cleanup
- Config herladen met `/clearlag reload`
- Aanpasbare berichten en prefix

## Vereisten

- Java 21
- Paper 1.20.6 of compatibele 1.20.x server
- Maven 3.9+ om te builden

## Builden

```bash
mvn clean package
```

De plugin jar wordt daarna aangemaakt in `target/`.

## Installatie

1. Build de plugin met Maven.
2. Plaats de gegenereerde jar in de `plugins/` map van je Paper server.
3. Start of herstart de server.
4. Pas daarna eventueel `plugins/ClearLag/config.yml` aan.

## Commando

```text
/clearlag reload
```

Herlaadt de configuratie zonder volledige server restart.

## Permissie

```text
clearlag.reload
```

Standaard alleen voor operators.

## Configuratie

Standaardconfig:

```yml
cleanup:
  start-delay-seconds: 900
  interval-seconds: 900

warnings:
  enabled: true
  times-seconds:
    - 60
    - 30
    - 10
    - 5
    - 4
    - 3
    - 2
    - 1

messages:
  broadcast: true
  prefix: "&b&lEternal &f&lSMP &7> "
  warning: "%prefix%&fDe grond-items worden over &b%time% &fopgeruimd."
  cleanup-complete: "%prefix%&fEr zijn &b%amount% items &fvan de grond verwijderd."
  reload-complete: "%prefix%&fDe ClearLag config is opnieuw geladen."
  reload-usage: "%prefix%&fGebruik: &b/clearlag reload"
```

## Werking

- Bij het opstarten plant de plugin een eerste cleanup in.
- Voor elk ingestelde waarschuwingstijdstip wordt een broadcast verstuurd.
- Tijdens de cleanup worden alle entities van het type `Item` uit alle werelden verwijderd.
- Daarna start automatisch de volgende cleanup-cyclus.

## Projectinfo

- Naam plugin: `ClearLag`
- Hoofdklasse: `com.shadow.ClearLagPlugin`
- Versie: `1.0.0`
- Auteur: `Liam`

## Opmerking

Deze plugin verwijdert alleen items die op de grond liggen. Mobs, minecarts, projectiles en andere entities worden niet geraakt.
