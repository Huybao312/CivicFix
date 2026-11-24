import {
  createContext,
  useContext,
  useEffect,
  useId,
  useRef,
  useState,
} from "react";
import type {
  FormEventHandler,
  ReactNode,
  ChangeEvent,
  FocusEvent,
  InvalidEvent,
} from "react";

const Announcement = createContext<(message: string) => void>(() => {});

export function ValidatedForm({
  children,
  onSubmit,
  label,
}: {
  children: ReactNode;
  onSubmit: FormEventHandler<HTMLFormElement>;
  label: string;
}) {
  const [announcement, setAnnouncement] = useState("");
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(
    () => () => {
      if (timer.current) clearTimeout(timer.current);
    },
    [],
  );
  function announce(message: string) {
    if (timer.current) clearTimeout(timer.current);
    setAnnouncement("");
    if (message)
      timer.current = setTimeout(() => setAnnouncement(message), 500);
  }
  return (
    <Announcement.Provider value={announce}>
      <form
        method="post"
        aria-label={label}
        onSubmit={onSubmit}
        onInvalidCapture={() => announce("")}
      >
        {children}
        <div className="visually-hidden" aria-live="polite" aria-atomic="true">
          {announcement}
        </div>
      </form>
    </Announcement.Provider>
  );
}

type Control = HTMLTextAreaElement | HTMLSelectElement;
type Props = {
  label: string;
  name: string;
  value: string;
  onChange: (value: string) => void;
  errorText: string;
  hint?: string;
  options?: { value: string; label: string }[];
  minLength?: number;
  maxLength?: number;
  disabled?: boolean;
  autoFocus?: boolean;
};
export function FormField({
  label,
  name,
  value,
  onChange,
  errorText,
  hint,
  options,
  minLength = 3,
  maxLength = 800,
  disabled,
  autoFocus,
}: Props) {
  const id = useId(),
    errorId = `${id}-error`,
    hintId = `${id}-hint`;
  const [invalid, setInvalid] = useState(false);
  const dirty = useRef(false);
  const announce = useContext(Announcement);
  function constraints(control: Control) {
    control.setCustomValidity(
      !options &&
        control.value.length > 0 &&
        control.value.trim().length < minLength
        ? errorText
        : "",
    );
  }
  function handleInput(e: ChangeEvent<Control>) {
    dirty.current = true;
    constraints(e.currentTarget);
    onChange(e.currentTarget.value);
    if (invalid && e.currentTarget.validity.valid) {
      setInvalid(false);
      announce("");
    }
    if (options) check(e.currentTarget, true);
  }
  function check(control: Control, speak: boolean) {
    constraints(control);
    const failed = !control.validity.valid;
    setInvalid(failed);
    if (speak) announce(failed ? `${label}. ${errorText}` : "");
  }
  function handleBlur(e: FocusEvent<Control>) {
    if (dirty.current || e.currentTarget.matches(":user-invalid"))
      check(e.currentTarget, true);
  }
  function handleInvalid(e: InvalidEvent<Control>) {
    check(e.currentTarget, false);
  }
  const attributes = {
    ref: (control: Control | null) => {
      // showModal reads the native autofocus attribute when it opens a closed dialog.
      if (control) control.autofocus = Boolean(autoFocus);
    },
    id,
    name,
    value,
    required: true,
    disabled,
    autoFocus,
    "aria-invalid": invalid,
    "aria-describedby":
      [hint ? hintId : "", invalid ? errorId : ""].filter(Boolean).join(" ") ||
      undefined,
    onChange: handleInput,
    onBlur: handleBlur,
    onInvalid: handleInvalid,
  };
  return (
    <div className="field validated-field">
      <label htmlFor={id}>
        {label} <span className="required-label">(bắt buộc)</span>
      </label>
      {hint && (
        <span id={hintId} className="field-hint">
          {hint}
        </span>
      )}
      {options ? (
        <select {...attributes}>
          <option value="" disabled>
            Chọn {label.toLocaleLowerCase("vi")}
          </option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : (
        <textarea
          {...attributes}
          minLength={minLength}
          maxLength={maxLength}
          rows={4}
        />
      )}
      <span id={errorId} className="field-error" hidden={!invalid}>
        {errorText}
      </span>
    </div>
  );
}
