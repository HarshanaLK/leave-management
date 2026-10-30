import {
  useId,
  useState,
  type ChangeEventHandler,
  type InputHTMLAttributes,
} from "react";
import { Eye, EyeOff } from "lucide-react";
import { FieldError } from "./FieldError";

type PasswordFieldProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "id" | "type" | "onChange" | "value"
> & {
  label: string;
  value: string;
  onChange: ChangeEventHandler<HTMLInputElement>;
  error?: string;
  help?: string;
};

/** Shared masked password control for login, account creation and password reset. */
export function PasswordField({
  label,
  value,
  onChange,
  error,
  help,
  required,
  placeholder = "Enter your password",
  ...inputProps
}: PasswordFieldProps) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  return (
    <div className="password-field">
      <label className="field-label" htmlFor={id}>
        {label}
        {required && (
          <span className="required" aria-hidden="true">
            *
          </span>
        )}
      </label>
      <div className="password-control">
        <input
          {...inputProps}
          id={id}
          type={visible ? "text" : "password"}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
          aria-invalid={!!error}
        />
        <button
          className="password-toggle"
          type="button"
          aria-label={visible ? "Hide password" : "Show password"}
          title={visible ? "Hide password" : "Show password"}
          aria-pressed={visible}
          onClick={() => setVisible((value) => !value)}
        >
          {visible ? (
            <EyeOff size={18} aria-hidden="true" />
          ) : (
            <Eye size={18} aria-hidden="true" />
          )}
        </button>
      </div>
      <FieldError message={error} />
      {help && <small className="form-help">{help}</small>}
    </div>
  );
}
