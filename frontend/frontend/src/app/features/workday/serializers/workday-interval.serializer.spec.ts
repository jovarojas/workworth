import {
  WorkdayIntervalSerializationError,
  localDateTimeToInstant,
  serializeWorkdayInterval
} from './workday-interval.serializer';

describe('workday interval serializer', () => {
  it('serializes a normal Europe/Madrid workday time to an instant', () => {
    expect(localDateTimeToInstant('2026-06-15', '08:00', 'Europe/Madrid')).toBe('2026-06-15T06:00:00.000Z');
  });

  it('serializes a July Europe/Madrid workday time to an instant', () => {
    expect(serializeWorkdayInterval('2026-07-06', '08:00', '10:30', 'Europe/Madrid')).toEqual({
      startedAt: '2026-07-06T06:00:00.000Z',
      endedAt: '2026-07-06T08:30:00.000Z'
    });
  });

  it('uses the winter Europe/Madrid offset after the daylight-saving transition', () => {
    expect(localDateTimeToInstant('2026-10-26', '08:00', 'Europe/Madrid')).toBe('2026-10-26T07:00:00.000Z');
  });

  it('rejects a nonexistent local time during the daylight-saving transition', () => {
    expect(() => localDateTimeToInstant('2026-03-29', '02:30', 'Europe/Madrid'))
      .toThrow(WorkdayIntervalSerializationError);
  });

  it('rejects an ambiguous local time during the return to winter time', () => {
    expect(() => localDateTimeToInstant('2026-10-25', '02:30', 'Europe/Madrid'))
      .toThrow(WorkdayIntervalSerializationError);
  });

  it('rejects malformed local date and time values', () => {
    expect(() => localDateTimeToInstant('2026-02-30', '08:00', 'Europe/Madrid'))
      .toThrow(WorkdayIntervalSerializationError);
    expect(() => localDateTimeToInstant('2026-08-12', '25:00', 'Europe/Madrid'))
      .toThrow(WorkdayIntervalSerializationError);
  });
});
