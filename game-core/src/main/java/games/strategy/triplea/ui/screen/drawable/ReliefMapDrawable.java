package games.strategy.triplea.ui.screen.drawable;

import java.awt.Image;

import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.UiContext;

public class ReliefMapDrawable extends MapTileDrawable {
  public ReliefMapDrawable(final int x, final int y, final UiContext uiContext) {
    super(x, y, uiContext);
  }

  @Override
  public MapTileDrawable getUnscaledCopy() {
    return new ReliefMapDrawable(x, y, uiContext);
  }

  @Override
  protected Image getImage() {
    if (noImage) {
      return null;
    }
    if (!TileImageFactory.getShowReliefImages()) {
      return null;
    }
    final Image image = uiContext.getTileImageFactory().getReliefTile(x, y);
    if (image == null) {
      noImage = true;
    }
    return image;
  }

  @Override
  public int getLevel() {
    return RELIEF_LEVEL;
  }
}
