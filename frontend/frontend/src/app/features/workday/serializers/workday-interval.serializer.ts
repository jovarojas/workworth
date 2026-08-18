export class WorkdayIntervalSerializationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'WorkdayIntervalSerializationError';
  }
}

export function serializeWorkdayInterval(
  localDate: string,
  startedAt: string,
  endedAt: string,
  timeZone: string
): { startedAt: string; endedAt: string } {
  return {
    startedAt: localDateTimeToInstant(localDate, startedAt, timeZone),
    endedAt: localDateTimeToInstant(localDate, endedAt, timeZone)
  };
}

export function localDateTimeToInstant(localDate: string, localTime: string, timeZone: string): string {
  const target = parseLocalDateTime(localDate, localTime);
  const targetMillis = Date.UTC(target.year, target.month - 1, target.day, target.hour, target.minute);
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23'
  });

  const candidates = [...offsetsNear(formatter, targetMillis)]
    .map((offset) => targetMillis - offset)
    .filter((candidate) => sameLocalDateTime(target, zonedParts(formatter, candidate)));

  if (candidates.length === 1) {
    return new Date(candidates[0]).toISOString();
  }

  if (candidates.length === 0) {
    throw new WorkdayIntervalSerializationError('La hora no existe en la zona horaria de la jornada.');
  }

  throw new WorkdayIntervalSerializationError('La hora es ambigua por el cambio horario de la jornada.');
}

type LocalDateTimeParts = {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
};

function parseLocalDateTime(localDate: string, localTime: string): LocalDateTimeParts {
  const date = /^(\d{4})-(\d{2})-(\d{2})$/.exec(localDate);
  const time = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(localTime);
  if (!date || !time) {
    throw new WorkdayIntervalSerializationError('La fecha u hora de la ausencia no es válida.');
  }

  const [year, month, day] = date.slice(1).map(Number);
  const [hour, minute] = time.slice(1).map(Number);
  const calendarDate = new Date(Date.UTC(year, month - 1, day));
  if (calendarDate.getUTCFullYear() !== year || calendarDate.getUTCMonth() !== month - 1 || calendarDate.getUTCDate() !== day) {
    throw new WorkdayIntervalSerializationError('La fecha de la ausencia no es válida.');
  }

  return { year, month, day, hour, minute };
}

function zonedParts(formatter: Intl.DateTimeFormat, instantMillis: number): LocalDateTimeParts {
  const parts = Object.fromEntries(
    formatter.formatToParts(new Date(instantMillis))
      .filter((part) => part.type !== 'literal')
      .map((part) => [part.type, Number(part.value)])
  );

  return {
    year: parts['year'],
    month: parts['month'],
    day: parts['day'],
    hour: parts['hour'],
    minute: parts['minute']
  };
}

function offsetsNear(formatter: Intl.DateTimeFormat, targetMillis: number): Set<number> {
  const offsets = new Set<number>();
  for (let hour = -26; hour <= 26; hour += 1) {
    const instantMillis = targetMillis + hour * 60 * 60 * 1_000;
    const observed = zonedParts(formatter, instantMillis);
    const observedMillis = Date.UTC(
      observed.year,
      observed.month - 1,
      observed.day,
      observed.hour,
      observed.minute
    );
    offsets.add(observedMillis - instantMillis);
  }
  return offsets;
}

function sameLocalDateTime(left: LocalDateTimeParts, right: LocalDateTimeParts): boolean {
  return left.year === right.year
    && left.month === right.month
    && left.day === right.day
    && left.hour === right.hour
    && left.minute === right.minute;
}
