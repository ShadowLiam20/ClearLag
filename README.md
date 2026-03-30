# ClearLag

Een eenvoudige ClearLag-plugin voor Paper servers. Deze plugin verwijdert automatisch items die op de grond liggen en kan vooraf waarschuwingen sturen naar alle spelers.

## Functies

- Automatische cleanup van grond-items
- Opruimen van XP-orbs en pijlen
- Optionele cleanup van andere projectiles, minecarts en boten
- Instelbare eerste delay en herhaal-interval
- Aankondigingen vlak voor de cleanup
- Handmatige cleanup met `/clearlag run`
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
/clearlag run
```

`reload` herlaadt de configuratie zonder volledige server restart.

`run` voert direct een cleanup uit met de huidige instellingen.

## Permissie

```text
clearlag.reload
clearlag.run
```

Beide staan standaard op operators.

## Configuratie

Standaardconfig:

```yml
cleanup:
  start-delay-seconds: 900
  interval-seconds: 900
  remove-items: true
  remove-xp-orbs: true
  remove-arrows: true
  remove-other-projectiles: false
  remove-minecarts: false
  remove-boats: false

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
  cleanup-complete: "%prefix%&fCleanup voltooid: &b%amount% &fentities verwijderd (&7%details%&f)."
  manual-cleanup-complete: "%prefix%&fHandmatige cleanup voltooid: &b%amount% &fentities verwijderd (&7%details%&f)."
  reload-complete: "%prefix%&fDe ClearLag config is opnieuw geladen."
  reload-usage: "%prefix%&fGebruik: &b/clearlag reload &7of &b/clearlag run"
  no-permission: "%prefix%&cJe hebt geen permissie voor dit commando."
```

## Werking

- Bij het opstarten plant de plugin een eerste cleanup in.
- Voor elk ingestelde waarschuwingstijdstip wordt een broadcast verstuurd.
- Tijdens de cleanup verwijdert de plugin standaard `Item`, `ExperienceOrb` en `AbstractArrow` entities uit alle werelden.
- Extra types zoals andere projectiles, minecarts en boten zijn via de config in te schakelen.
- Daarna start automatisch de volgende cleanup-cyclus.

## Projectinfo

- Naam plugin: `ClearLag`
- Hoofdklasse: `com.shadow.ClearLagPlugin`
- Versie: `1.0.0`
- Auteur: `Liam`

## Opmerking

Deze plugin kan entity-lag verminderen, maar kan niet "alle lag" oplossen. TPS-problemen door redstone, chunk loading, plugins, mobs, databases of slechte hosting moet je apart aanpakken.
