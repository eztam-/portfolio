package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerYearChartTab extends AbstractChartTab
{
    private class DividendPerYearToolTip extends TimelineChartToolTip
    {
        private PaymentsViewModel model;

        public DividendPerYearToolTip(Chart chart, PaymentsViewModel model)
        {
            super(chart);

            this.model = model;
        }

        @Override
        protected void createComposite(Composite parent)
        {
            final int year = (Integer) getFocusedObject();
            int totalNoOfMonths = model.getNoOfMonths();

            Color barColor = Colors.DARK_BLUE;

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().getSeries()[0];
            if (barSeries.isStackEnabled())
            {
                barSeries = (IBarSeries) getChart().getSeriesSet().getSeries()[year];
                barColor = barSeries.getBarColor();
            }

            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = year * 12; index < (year + 1) * 12
                                                && index < totalNoOfMonths; index += 1)
                                    if (line.getValue(index) != 0L)
                                        return true;
                                return false;
                            })
                            .sorted((l1, l2) -> l1.getVehicle().getName()
                                            .compareToIgnoreCase(l2.getVehicle().getName()))
                            .collect(Collectors.toList());

            final Composite container = new Composite(parent, SWT.NONE);
            container.setBackgroundMode(SWT.INHERIT_FORCE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

            Color foregroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
            container.setForeground(foregroundColor);
            container.setBackground(Colors.INFO_TOOLTIP_BACKGROUND);

            Label topLeft = new Label(container, SWT.NONE);
            topLeft.setForeground(foregroundColor);
            topLeft.setText(Messages.ColumnSecurity);

            Label label = new Label(container, SWT.CENTER);
            label.setBackground(barColor);
            label.setForeground(Colors.getTextColor(barSeries.getBarColor()));
            label.setText(TextUtil.pad(String.valueOf(model.getStartYear() + year)));
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(label);

            lines.forEach(line -> {
                Label l = new Label(container, SWT.NONE);
                l.setForeground(foregroundColor);
                l.setText(TextUtil.tooltip(line.getVehicle().getName()));

                long value = 0;
                for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                    value += line.getValue(m);

                l = new Label(container, SWT.RIGHT);
                l.setForeground(foregroundColor);
                l.setText(TextUtil.pad(Values.Amount.format(value)));
                GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
            });

            if (model.usesConsolidateRetired())
            {
                Label lSumRetired = new Label(container, SWT.NONE);
                lSumRetired.setForeground(foregroundColor);
                lSumRetired.setText(Messages.LabelPaymentsConsolidateRetired);

                long value = 0;
                for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                    value += model.getSumRetired().getValue(m);

                Label cl = new Label(container, SWT.RIGHT);
                cl.setForeground(foregroundColor);
                cl.setText(TextUtil.pad(Values.Amount.format(value)));
                GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(cl);
            }

            Label lSum = new Label(container, SWT.NONE);
            lSum.setForeground(foregroundColor);
            lSum.setText(Messages.ColumnSum);

            long value = 0;
            for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                value += model.getSum().getValue(m);

            Label l = new Label(container, SWT.RIGHT);
            l.setBackground(barSeries.getBarColor());
            l.setForeground(Colors.getTextColor(barSeries.getBarColor()));
            l.setText(TextUtil.pad(Values.Amount.format(value)));
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsPerYear;
    }

    @Override
    protected void attachTooltipTo(Chart chart)
    {
        DividendPerYearToolTip toolTip = new DividendPerYearToolTip(chart, model);
        toolTip.enableCategory(true);
    }

    private void updateCategorySeries()
    {
        int startYear = model.getStartYear();
        String[] labels = new String[LocalDate.now().getYear() - startYear + 1];
        for (int ii = 0; ii < labels.length; ii++)
            labels[ii] = String.valueOf(startYear + ii);
        getChart().getAxisSet().getXAxis(0).setCategorySeries(labels);
    }

    @Override
    protected void createSeries()
    {
        updateCategorySeries();

        int startYear = model.getStartYear();

        double[] series = new double[LocalDate.now().getYear() - startYear + 1];

        boolean hasNegativeNumber = false;

        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = (index / 12);

            long total = 0;

            int months = Math.min(12, model.getNoOfMonths() - index);
            for (int ii = 0; ii < months; ii++)
                total += model.getSum().getValue(index + ii);

            series[year] = total / Values.Amount.divider();

            if (total < 0L)
                hasNegativeNumber = true;
        }

        if (hasNegativeNumber)
        {
            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR, getLabel());
            barSeries.setYSeries(series);
            barSeries.setBarColor(Colors.DARK_BLUE);
        }
        else
        {
            for (int i = 0; i < series.length; i++)
            {
                int year = model.getStartYear() + i;
                IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR,
                                String.valueOf(year));

                double[] seriesX = new double[LocalDate.now().getYear() - startYear + 1];
                seriesX[i] = series[i];

                barSeries.setYSeries(seriesX);

                barSeries.setBarColor(getColor(year));
                barSeries.setBarPadding(25);
                barSeries.enableStack(true);
            }
        }
    }
}