package com.tradable.algo.macd;

import java.util.Collection;

import com.tradable.api.algo.indicator.data.IndicatorOutput;
import com.tradable.api.algo.indicator.data.NumericPlotValue;
import com.tradable.api.algo.strategy.*;
import com.tradable.api.entities.OrderDuration;
import com.tradable.api.entities.OrderSide;
import com.tradable.api.entities.Position;

public class MACDStrategy extends Strategy {
	private final StrategyPreset preset;

	private int tradeDuration = 600;
	
	private long longOpenTime = 0;
	private long shortOpenTime = 0;
	private int tradeSize = 100000;
	
	public MACDStrategy(StrategyPreset preset) {
		this.preset = preset;
	}

	private int longPositionCount(StrategyContext context) {
		int count = 0;
		Collection<Position> positions = context.getOwnPositions();
		for (Position p : positions) {
			if (p.getQuantity() > 0) {
				count++;
			}
		}
		return count;
	}
	
	private int shortPositionCount(StrategyContext context) {
		int count = 0;
		Collection<Position> positions = context.getOwnPositions();
		for (Position p : positions) {
			if (p.getQuantity() < 0) {
				count++;
			}
		}
		return count;
	}
	
	private void openLong(StrategyContext context) {
		int result = context.placeMarket(preset.getSymbol(), OrderSide.BUY, tradeSize, OrderDuration.DAY);
		if (result == 0) {
			longOpenTime = System.currentTimeMillis();
		}
	}
	
	private void closeLong(StrategyContext context) {
		if (longPositionCount(context) > 0) {
			int result = context.placeMarket(preset.getSymbol(), OrderSide.SELL, tradeSize, OrderDuration.DAY);
			if (result == 0) {
				// closed successfully
			}
		}
	}
	
	private void openShort(StrategyContext context) {
		int result = context.placeMarket(preset.getSymbol(), OrderSide.SELL, tradeSize, OrderDuration.DAY);
		if (result == 0) {
			shortOpenTime = System.currentTimeMillis();
		}
	}
	
	private void closeShort(StrategyContext context) {
		if (shortPositionCount(context) > 0) {
			int result = context.placeMarket(preset.getSymbol(), OrderSide.BUY, tradeSize, OrderDuration.DAY);
			if (result == 0) {
				// closed successfully
			}
		}
	}

	private int oldLongs = 0;
	private int oldShorts = 0;
	
	@Override
	public void recalculate(StrategyContext context) {

		// count the positions
		int longs = longPositionCount(context);
		int shorts = shortPositionCount(context);
	
		// avoid spamming setStatusMessage() by storing the previous status
		if (longs == 0 && shorts == 0 && (oldLongs != 0 || oldShorts != 0)) {
			context.setStatusMessage("Waiting for a trading signal...");
		}
		else if (longs > 0 && oldLongs != longs) {
			context.setStatusMessage("Opened long position.");
		}
		else if (shorts > 0 && oldShorts != shorts) {
			context.setStatusMessage("Opened short position.");
		}
		oldLongs = longs;
		oldShorts = shorts;
		
		// get the MACD indicator values
		IndicatorOutput output =  context.getLastIndicatorValue(0);
		double macd = ((NumericPlotValue)output.getValue(0)).getValue();
		double macdAvg = ((NumericPlotValue)output.getValue(1)).getValue();
		
		if (longs == 0 && shorts == 0) {
			// no positions opened
			if (macd > 0 && macdAvg < 0) { // long opportunity
				openLong(context);
			}
			else if (macd < 0 && macdAvg > 0) { // short opportunity
				openShort(context);
			}
		}
		else {
			// manage the ongoing position
			if (longs > 0) {
				// ongoing long position
				if (macd < 0 && System.currentTimeMillis() - longOpenTime > 1000 * tradeDuration) {
					closeLong(context);
				}
			}
			if (shorts > 0) {
				// ongoing short position
				if (macd > 0 && System.currentTimeMillis() - shortOpenTime > 1000 * tradeDuration) {
					closeShort(context);
				}
			}
		}
	}

	@Override
	public void onStart(StrategyContext context) {
		// read the parameters
		tradeSize = (Integer) preset.getParameters().get("tradeSize");
		tradeDuration = 60 * (Integer) preset.getParameters().get("tradeDuration");
	}

	@Override
	public void onStop(StrategyContext context) {
		// since this is merely a sample, the positions are closed when stopping
		context.closeOwnPositions();
	}
}
