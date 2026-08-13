import { CommonModule, CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, ElementRef, OnDestroy, OnInit, effect, inject, signal, viewChildren } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { finalize } from 'rxjs';
import { problemDetailMessage } from '../../../../core/http/problem-detail';
import { statisticsGranularityLabel } from '../../../../core/presentation/display-labels';
import {
  StatisticAvailability,
  StatisticsGranularity,
  StatisticsPointResponse,
  StatisticsResponse
} from '../../../../core/models/workworth-api.models';
import { StatisticsApiService } from '../../../../core/services/statistics-api.service';

Chart.register(...registerables);

@Component({
  selector: 'app-statistics-page',
  imports: [
    CommonModule,
    CurrencyPipe,
    DecimalPipe,
    MatButtonToggleModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './statistics-page.component.html',
  styleUrl: './statistics-page.component.scss'
})
export class StatisticsPageComponent implements OnInit, OnDestroy {
  private readonly statisticsApi = inject(StatisticsApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly chartCanvases = viewChildren<ElementRef<HTMLCanvasElement>>('statisticsChart');
  private readonly charts: Chart[] = [];
  private requestGeneration = 0;

  readonly granularities: StatisticsGranularity[] = ['DAY', 'WEEK', 'MONTH', 'YEAR'];
  readonly granularity = signal<StatisticsGranularity>('MONTH');
  readonly statistics = signal<StatisticsResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const result = this.statistics();
      const canvases = this.chartCanvases();
      if (!result || canvases.length !== 4) {
        return;
      }
      this.renderCharts(result.points, canvases);
    });
  }

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  selectGranularity(granularity: StatisticsGranularity): void {
    if (granularity === this.granularity()) {
      return;
    }
    this.granularity.set(granularity);
    this.load();
  }

  metricAvailable(status: StatisticAvailability): boolean {
    return status === 'AVAILABLE';
  }

  granularityLabel(granularity: StatisticsGranularity): string {
    return statisticsGranularityLabel(granularity);
  }

  private load(): void {
    const generation = ++this.requestGeneration;
    this.loading.set(true);
    this.error.set(null);
    this.statisticsApi.statistics(this.granularity())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (generation === this.requestGeneration) {
            this.loading.set(false);
          }
        })
      )
      .subscribe({
        next: (statistics) => {
          if (generation === this.requestGeneration) {
            this.statistics.set(statistics);
          }
        },
        error: (error: unknown) => {
          if (generation === this.requestGeneration) {
            this.error.set(this.errorMessage(error));
          }
        }
      });
  }

  private renderCharts(points: StatisticsPointResponse[], canvases: readonly ElementRef<HTMLCanvasElement>[]): void {
    if (typeof CanvasRenderingContext2D === 'undefined') {
      return;
    }
    this.destroyCharts();
    const labels = points.map((point) => point.startDate);
    const series = [
      { label: 'Horas económicas efectivas', values: points.map((point) => this.valueForChart(point.workedHours.status, point.workedHours.value)), color: '#4658c4' },
      { label: 'Salario medio efectivo por hora', values: points.map((point) => this.valueForChart(point.averageHourlyEarnings.status, point.averageHourlyEarnings.amount)), color: '#8c4fc8' },
      { label: 'Ganancias efectivas totales', values: points.map((point) => this.valueForChart(point.totalEarnings.status, point.totalEarnings.amount)), color: '#067647' },
      { label: 'Objetivos completados', values: points.map((point) => this.valueForChart(point.completedGoals.status, point.completedGoals.count)), color: '#b54708' }
    ];

    series.forEach((entry, index) => {
      const context = canvases[index].nativeElement.getContext('2d');
      if (!context) {
        return;
      }
      const configuration: ChartConfiguration<'line', Array<number | null>, string> = {
        type: 'line',
        data: {
          labels,
          datasets: [{
            label: entry.label,
            data: entry.values,
            borderColor: entry.color,
            backgroundColor: entry.color,
            spanGaps: false,
            tension: 0,
            pointRadius: 3
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { x: { ticks: { maxRotation: 0, autoSkip: true } }, y: { beginAtZero: true } }
        }
      };
      this.charts.push(new Chart(context, configuration));
    });
  }

  private valueForChart(status: StatisticAvailability, value: number | null): number | null {
    return status === 'AVAILABLE' ? value : null;
  }

  private destroyCharts(): void {
    this.charts.splice(0).forEach((chart) => chart.destroy());
  }

  private errorMessage(error: unknown): string {
    const detail = problemDetailMessage(error);
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth para cargar las estadísticas.';
    }
    return 'No se han podido cargar las estadísticas. Inténtalo de nuevo más tarde.';
  }
}
