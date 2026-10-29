from pathlib import Path

java = Path("app/src/main/java/it/fantozziforever/player/MainActivity.java").read_text(encoding="utf-8")
layout = Path("app/src/main/res/layout/activity_main.xml").read_text(encoding="utf-8")
row = Path("app/src/main/res/layout/episode_row.xml").read_text(encoding="utf-8")
for forbidden in ('onlyFavorites', 'R.id.favorites', 'favorites.setOnClickListener', '★ Preferite', '☆ Tutte', 'Mostra tutte'):
    assert forbidden not in java, f"Filtro presente: {forbidden}"
assert '@+id/favorites' not in layout
assert 'R.id.star' in java and '@+id/star' in row
assert 'CatalogTitles.TITLES[i].toLowerCase(Locale.ITALIAN).contains(q)' in java
print("OK: filtro rimosso, ricerca e preferiti mantenuti")
