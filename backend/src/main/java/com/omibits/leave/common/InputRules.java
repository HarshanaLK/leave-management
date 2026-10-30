package com.omibits.leave.common;

/** Shared field limits mirrored by frontend/src/lib/validation.ts. */
public final class InputRules {

  private InputRules() {}

  public static final String PERSON_NAME =
    "^[\\p{L}\\p{M}]+(?:[ .'-][\\p{L}\\p{M}]+)*$";
  public static final String DEPARTMENT_NAME =
    "^[\\p{L}\\p{M}\\p{N}](?:[\\p{L}\\p{M}\\p{N} &()'./-]*[\\p{L}\\p{M}\\p{N})])?$";
  // Password must have uppercase, lowercase, numeric and symbol characters; no whitespace.
  public static final String STRONG_PASSWORD =
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{8,72}$";
  public static final int SEARCH_MAX = 120;
}
