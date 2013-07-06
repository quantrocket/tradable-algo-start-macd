package com.tradable.algo.macd;

import java.util.Arrays;
import java.util.List;

import com.tradable.api.algo.dataseries.*;
import com.tradable.api.algo.descriptor.parameter.IntegerParameterDescriptor;
import com.tradable.api.algo.descriptor.parameter.ParameterDescriptor;
import com.tradable.api.algo.strategy.Strategy;
import com.tradable.api.algo.strategy.StrategyDescriptor;
import com.tradable.api.algo.strategy.StrategyFactory;
import com.tradable.api.algo.strategy.StrategyInitializingContext;
import com.tradable.api.algo.strategy.StrategyPreset;
import com.tradable.api.services.historicmarketdata.HistoricSymbol;

public class MACDStrategyFactory implements StrategyFactory {

	private StrategyDescriptor descriptor;
	
	public MACDStrategyFactory() {
		// set up the parameter list
		List<? extends ParameterDescriptor> parameters = Arrays.asList(
				new IntegerParameterDescriptor("tradeSize", "trade amount", 100000, 1, Integer.MAX_VALUE),
				new IntegerParameterDescriptor("tradeDuration", "min trade duration (minutes)", 10, 1, 100)

		);
		// create the algo descriptor
		descriptor = new StrategyDescriptor("MACDStrategy", "MACD algo sample", null, parameters);
	}
	
	@Override
	public DataSeriesFeed<?> createDataFeed(DataSeriesKey dataSeriesKey) {
		return null;
	}

	@Override
	public StrategyDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public Strategy create(StrategyPreset preset,
			StrategyInitializingContext context) {
		// subscribe to the tick feed for the configured symbol
		context.subscribeQuoteTick(preset.getSymbol());
		// set up the indicator data feed 
		HistoricSymbol sh = new HistoricSymbol(preset.getSymbol());
		context.useForIndicators(new HistoryDataSeriesKey(sh, System.currentTimeMillis() - 60 * 60 * 1000));
		// set up the indicator subscription
		context.subscribeIndicator(context.MACD(10, 20, 10).build());
		return new MACDStrategy(preset);
	}

}
