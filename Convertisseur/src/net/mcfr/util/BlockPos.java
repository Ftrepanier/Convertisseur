package net.mcfr.util;

public class BlockPos {
  private final int x, y, z;

  public BlockPos(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int getZ() {
    return this.z;
  }

  @Override
  public String toString() {
    return String.format("(%d, %d, %d)", getX(), getY(), getZ());
  }
}
