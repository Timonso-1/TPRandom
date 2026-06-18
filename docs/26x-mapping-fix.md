# Mapping-Fix für Minecraft 26.x (26.1.2)

Dieses Dokument erklärt, warum der Mapping-Fehler beim Bauen gegen
Minecraft `26.1.2` auftrat und welche Änderungen ihn beheben.

## Das Problem

Minecraft 26.x wird **unobfuskiert** ausgeliefert: Die Klassen-, Methoden-
und Feldnamen liegen bereits im Klartext vor.

Damit gibt es zwei Dinge nicht mehr, auf die Fabric Loom standardmäßig
angewiesen ist:

- keinen `intermediary`-Namespace (die stabile Zwischenbenennung, die Fabric
  sonst zwischen Versionen verwendet)
- keine offiziellen Mojang-Mappings zum Herunterladen/Anwenden

Die alte Build-Konfiguration zwang Loom aber genau dazu:

```kotlin
mappings(loom.officialMojangMappings())
```

Loom versuchte also, nicht existierende Mappings zu laden und gegen einen
nicht vorhandenen `intermediary`-Namespace zu remappen. Das löste den
Mapping-Fehler aus.

## Die Lösung

### 1. Loom-Version anheben

```diff
- id("fabric-loom") version "1.16-SNAPSHOT"
+ id("fabric-loom") version "1.17.0-alpha.14"
```

Erst ab dieser Version kennt Loom die API `noIntermediateMappings()`.

### 2. Intermediary-Mappings abschalten

```kotlin
loom {
    splitEnvironmentSourceSets()

    // Minecraft 26.x ships unobfuscated and has no intermediary/official mappings.
    @Suppress("UnstableApiUsage")
    noIntermediateMappings()
}
```

Sagt Loom: Es gibt keinen `intermediary`-Namespace – also nicht danach
suchen und nicht dorthin remappen.

### 3. Offizielle Mappings durch ein Identitäts-Mapping ersetzen

```diff
- mappings(loom.officialMojangMappings())
+ mappings(loom.layered {
+     mappings(file("mappings/identity.tiny"))
+ })
```

Statt nicht existierender Mojang-Mappings wird eine eigene, leere
„Identitäts"-Mappingdatei verwendet.

### 4. Datei `mappings/identity.tiny`

```
tiny	2	0	official	named
```

Nur der Tiny-v2-Header, keine Einträge. Das definiert `official` → `named`
als 1:1-Abbildung. Da das Spiel bereits Klartextnamen hat, ist keine
Umbenennung nötig – `official` und `named` sind identisch.

> Wichtig: Die Trennung im Header muss ein **Tab** sein, kein Leerzeichen.

### 5. `withSourcesJar()` entfernen

```diff
- withSourcesJar()
+ // No sourcesJar: MC 26.x is unobfuscated with no intermediary namespace,
+ // so Loom cannot remap a sources jar (remapSourcesJar requires "intermediary").
```

Der `remapSourcesJar`-Task braucht zwingend einen `intermediary`-Namespace.
Ohne den kann Loom kein Sources-Jar remappen, deshalb wird es deaktiviert.

## Begleitende Änderungen (nicht mapping-relevant)

- Gradle-Wrapper `9.2.1` → `9.5.1`
- Java-Toolchain auf Version `25`
- In `processResources` die Properties explizit `as String` gecastet
  (Typsicherheit beim `expand`)

## Kurzfassung

Der Fehler trat auf, weil Loom für ein unobfuskiertes 26.x weiterhin nach
`intermediary`-/Mojang-Mappings suchte. Mit `noIntermediateMappings()` plus
einer leeren `identity.tiny` als Identitäts-Mapping entfällt dieser Schritt
vollständig – und damit der Fehler.
