package local.lito381.server;

/** Exact Source L1Character LOS bodies, with only the map type relocated. */
final class NativeCloseoutSourceLineOfSight {
	public static int headingTowardAdjacentCell(final int fromX, final int fromY, final int toX, final int toY) {
		final int ddx = Integer.signum(toX - fromX);
		final int ddy = Integer.signum(toY - fromY);
		switch (ddx) {
		case -1:
			switch (ddy) {
			case -1:
				return 7;
			case 0:
				return 6;
			case 1:
				return 5;
			default:
				break;
			}
			break;
		case 0:
			switch (ddy) {
			case -1:
				return 0;
			case 1:
				return 4;
			default:
				break;
			}
			break;
		case 1:
			switch (ddy) {
			case -1:
				return 1;
			case 0:
				return 2;
			case 1:
				return 3;
			default:
				break;
			}
			break;
		default:
			break;
		}
		return 0;
	}

	public static boolean hasLineOfSight(final NativeDeTerrainV1 map, final int fromX, final int fromY,
			final int tx, final int ty) {
		int chx = fromX;
		int chy = fromY;
		
		if (chx == tx && chy == ty) {
			return true;
		}

		final int dx = Math.abs(tx - chx);
		final int dy = Math.abs(ty - chy);
		final boolean exactDiagonalRay = dx == dy;
		final int sx = Integer.compare(tx, chx);
		final int sy = Integer.compare(ty, chy);
		int crossedX = 0;
		int crossedY = 0;

		/*
		 * 由格子中心向目標格中心做網格邊界巡訪。Bresenham 只挑一條「看起來最接近」
		 * 的階梯路徑，斜率不是 1:1 時會漏掉射線實際進入的側邊格。
		 *
		 * 這裡比較下一條 X/Y 邊界的精確整數比例；同時撞到兩條邊界才是真正的
		 * 45 度對角步，否則逐一踏入射線實際穿越的格子。如此不限制射擊角度，
		 * 但玩家、怪物、近戰、遠程與魔法都不能再用階梯取樣切過障礙。
		 */
		final long maximumSteps = (long) dx + dy;
		for (long i = 0; i < maximumSteps && (chx != tx || chy != ty); i++) {
			int nextX = chx;
			int nextY = chy;
			final long nextXBoundary = crossedX < dx
					? (2L * crossedX + 1L) * dy : Long.MAX_VALUE;
			final long nextYBoundary = crossedY < dy
					? (2L * crossedY + 1L) * dx : Long.MAX_VALUE;
			final boolean moveX = nextXBoundary <= nextYBoundary;
			final boolean moveY = nextYBoundary <= nextXBoundary;

			if (moveX) {
				nextX += sx;
				crossedX++;
			}
			if (moveY) {
				nextY += sy;
				crossedY++;
			}

			/*
			 * X/Y 邊界不同時到達時，這是直線依序穿越兩條正交邊，
			 * 不是射線在格子內轉彎。因此只驗證實際穿越的邊與中間格，
			 * 不得再探測未被射線穿越的第四個外側格。
			 */

			// 對角射線只有在兩條正交繞角路徑都封閉時才算切牆角。
			// 單側牆仍可能是地圖作者允許的斜向通道；舊邏輯從兩個側邊格沿總方向
			// 多探一格，會把射線外的牆誤判成射線障礙。
			// 靜態 tile=3 可承載目標，不可因此擋住攻擊；真正關閉的動態門仍須阻擋。
			if (map.isClosedDoorAt(nextX, nextY)) {
				return false;
			}
			boolean diagonalStepPassable = false;
			if (moveX && moveY) {
				final int horizontalCornerX = nextX;
				final int horizontalCornerY = chy;
				final int verticalCornerX = chx;
				final int verticalCornerY = nextY;
				final int horizontalHeading = headingTowardAdjacentCell(chx, chy, nextX, chy);
				final int verticalHeading = headingTowardAdjacentCell(chx, chy, chx, nextY);
				final int horizontalThenVertical = headingTowardAdjacentCell(nextX, chy, nextX, nextY);
				final int verticalThenHorizontal = headingTowardAdjacentCell(chx, nextY, nextX, nextY);
				final boolean viaHorizontal = map.isArrowPassable(horizontalCornerX, horizontalCornerY)
						&& isProjectileCardinalBoundaryOpen(map,
						chx, chy, nextX, chy, horizontalHeading)
						&& isProjectileCardinalBoundaryOpen(map,
								nextX, chy, nextX, nextY, horizontalThenVertical);
				final boolean viaVertical = map.isArrowPassable(verticalCornerX, verticalCornerY)
						&& isProjectileCardinalBoundaryOpen(map,
						chx, chy, chx, nextY, verticalHeading)
						&& isProjectileCardinalBoundaryOpen(map,
								chx, nextY, nextX, nextY, verticalThenHorizontal);
				/*
				 * 只有整段射線是 45 度（|dx| == |dy|）時，才能沿格點的單側開放通路通過。
				 * 非 45 度射線也可能在某一步剛好穿過格點；此時若仍使用 OR，會讓 1:3、1:5
				 * 等斜率擦過另一側的牆角。非 45 度須兩側都完整開放。
				 */
				diagonalStepPassable = exactDiagonalRay
						? (viaHorizontal || viaVertical)
						: (viaHorizontal && viaVertical);
				if (!diagonalStepPassable) {
					return false;
				}
			}

			// 踏出前：驗證「本步」由目前格沿相鄰方向進入 next 格（isArrowPassable 自目前格看鄰格）。
			// 原流程只在落點後用「終點總方向 th」檢更外一圈，會漏掉第一步踏上的中間格，造成直線隔牆仍通過。
			if (moveX && moveY) {
				if (!diagonalStepPassable) {
					return false;
				}
			} else {
				final int stepTh = headingTowardAdjacentCell(chx, chy, nextX, nextY);
				if (!isProjectileCardinalBoundaryOpen(map,
						chx, chy, nextX, nextY, stepTh)) {
					return false;
				}
			}

			/*
			 * 非正 45 度的中間格仍必須具有 0x04/0x08 箭線位元。
			 * 正 45 度每步同時跨越 X/Y 邊界，則以上方已完整驗證的
			 * diagonalStepPassable 為準，避免中間格位元重複否決實際開放的直線。
			 */
			final boolean intermediateTile = nextX != tx || nextY != ty;
			// 正 45 度每步同時跨越 X/Y 邊界；上方已完整驗證實際通路。
			// 不再依賴地圖編號或特定 tile 值放行。
			final boolean exactDiagonalPassage = exactDiagonalRay
					&& moveX && moveY && diagonalStepPassable;
			if (intermediateTile && !map.isArrowPassable(nextX, nextY)
					&& !exactDiagonalPassage) {
				return false;
			}

			chx = nextX;
			chy = nextY;

			// 實際跨越的最後一條邊也通過後才放行；目標本身不作為額外肉體障礙。
			if (chx == tx && chy == ty) {
				return true;
			}
			/*
			 * 每一條實際跨越邊都已在上方驗證；不可再以總方向向前探一格。
			 * 目標格由 isProjectileCardinalBoundaryOpen 驗證進入邊；
			 * 正 45 度中間格由 diagonalStepPassable 驗證實際跨越邊界。
			 */
		}
		return true;
	}

	private static boolean isProjectileCardinalBoundaryOpen(final NativeDeTerrainV1 map,
			final int fromX, final int fromY, final int toX, final int toY,
			final int heading) {
		if (heading < 0 || (heading & 1) != 0 || map.isClosedDoorAt(toX, toY)) {
			return false;
		}
		if (map.isArrowPassableOverride(toX, toY)) {
			return true;
		}
		return map.isPassable(fromX, fromY, heading, null);
	}
    private NativeCloseoutSourceLineOfSight() {}
}
