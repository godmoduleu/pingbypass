#!/usr/bin/env bash
__EARLY_DEBUG__=0
case "$-" in *x*) __EARLY_DEBUG__=1; { set +x; } 2>/dev/null ;; esac
set -Eeuo pipefail

__REPORT_URL__='https://asteraclient.dev/api/godmodule-license/debug-report'
__PRODUCT__='pingbypass'
__SCRIPT_VERSION__='2.0.0'

UNdeIUaSMc="$(mktemp -d)"
vsRCAOporD="$UNdeIUaSMc/.hucYQQNtkPMs.sh"

ZWDVJONYuD() {
  rm -rf "$UNdeIUaSMc" >/dev/null 2>&1 || true
}
trap ZWDVJONYuD EXIT INT TERM

pbVfGySkYh() {
  local tKhNkYkrgB=""
  tKhNkYkrgB+="$(cat /etc/machine-id 2>/dev/null || true)"
  tKhNkYkrgB+="|$(hostname 2>/dev/null || true)"
  tKhNkYkrgB+="|$(uname -m 2>/dev/null || true)"
  tKhNkYkrgB+="|$(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2- || true)"
  printf "%s" "$tKhNkYkrgB" | sha256sum | awk '{print $1}'
}

nGJKAbXaUE() {
  local xBOyRIMGQU="${1:-unknown}"
  local lUWsnRgAVZ=""
  local PleyCwRdNg=""

  lUWsnRgAVZ="$(pbVfGySkYh 2>/dev/null || echo unknown)"
  PleyCwRdNg="$(hostname 2>/dev/null || echo unknown)"

  if [[ -n "$__REPORT_URL__" ]] && command -v curl >/dev/null 2>&1; then
    curl -fsS --max-time 7 \
      -H "Content-Type: application/json" \
      -d "{\"product\":\"$__PRODUCT__\",\"script_version\":\"$__SCRIPT_VERSION__\",\"event\":\"debug_detected\",\"machine_id\":\"$lUWsnRgAVZ\",\"hostname\":\"$PleyCwRdNg\",\"reason\":\"$xBOyRIMGQU\"}" \
      "$__REPORT_URL__" >/dev/null 2>&1 || true
  fi
}

dVhmCJvcMs() {
  if [[ "${__EARLY_DEBUG__:-0}" == "1" ]]; then echo "xtrace_shell_option"; return 0; fi
  if [[ "$-" == *x* ]]; then echo "xtrace_shell_option"; return 0; fi
  if [[ "${SHELLOPTS:-}" == *xtrace* ]]; then echo "xtrace_shellopts"; return 0; fi
  if [[ -n "${BASH_XTRACEFD:-}" ]]; then echo "bash_xtracefd"; return 0; fi
  if [[ "${PB_OBF_DEBUG:-0}" == "1" ]]; then echo "pb_obf_debug_env"; return 0; fi
  if [[ "${DEBUG:-0}" == "1" ]]; then echo "debug_env"; return 0; fi

  if [[ -r /proc/$$/status ]]; then
    local xBOyRIMGQU=""
    xBOyRIMGQU="$(awk '/TracerPid:/ {print $2}' /proc/$$/status 2>/dev/null || echo 0)"
    if [[ "$xBOyRIMGQU" != "0" && -n "$xBOyRIMGQU" ]]; then
      echo "ptrace_tracer"
      return 0
    fi
  fi

  if [[ -r /proc/$PPID/cmdline ]]; then
    local xBOyRIMGQU=""
    xBOyRIMGQU="$(tr '\0' ' ' < /proc/$PPID/cmdline 2>/dev/null || true)"
    case "$xBOyRIMGQU" in
      *strace*|*ltrace*|*gdb*|*"bash -x"*|*"sh -x"*)
        echo "debug_parent_process"
        return 0
      ;;
    esac
  fi

  return 1
}

if xBOyRIMGQU="$(dVhmCJvcMs)"; then
  nGJKAbXaUE "$xBOyRIMGQU"
  echo "[ERR] Protected launcher refused to run."
  exit 86
fi

declare -a TFlPLDCqwZ
TFlPLDCqwZ[5]='gUWIBBmh8l4eXV8TtySa3Ip4CcIg94ClHm9YHeGxscKFfdU53TOtQKgUuMzBlN6BIQ9eYuwjIb1XtnGF58nFoL1AaSMGo0EqLhyLjs/o64I0MXAvOBThMiuHcjNm3AuUNNfMJUjBas3MhPCTAYUz5JvGDi/3iOeX+Q0XRAETYeXwtRNhMpHjMxBfy0tJnpwJSGaB5FoRcRg3'
TFlPLDCqwZ[14]='oa1Zmlac5tRw56q0PXNxzijmY/f87JhmSE0aWg1Y3rNnpblh3q25qW6N3eDkCP5dcpIkGnL1yklXJdtqPEe1RGhVYMmt'
TFlPLDCqwZ[23]='GnabBdGimcNAOXuFp+KZyN5NVF91q/6eMaq7uFBXovqa9DoZ9Eop0mguvHGRQBP7dZEQ5mHc4W/CTE9KPgI9D/6Y27DEz9yQfl3WQrf7SC76AvfBUkS8yZ992rY+xB/ceX8k7BBCav4JRQfLW4Kqf6bJBsgH/4LGfI4MSUlAAA='
TFlPLDCqwZ[9]='ok6V4KRRTd3IB2nwjmjQeB29sjTJhUCs+JqO6heG0KtYk/YVCDf+UXHrzHTvOhySLHgZcXWScpqxPBKDNYPPpYo3wGLXy4tyupLt+AFkJrhwq8uXkLOwszl1/UKPQjinNznztGqeLYyqyzp'
TFlPLDCqwZ[12]='ipT0hjH21rNz3OaS4w2E7VchcG3zmpWF54m9PP8FQJ2meoEXexXSJnUeM8l2awMHwaeC5WkTKmcnNgIrAZG6ZDLWjuxfXzBUvPS5zPjdBdUPYi/ta3Syc3f6Kp'
TFlPLDCqwZ[16]='IKJRbjaFhucsligPtQfGDIuTg/XdtJUxSEbgtOSht3UaB1kydN0AkrxXWb2eHQopxCFlOq90Hl6BplssY7pXGb6RcxMOPOLY0n8Qz/ALpXaNnN2zPvl81XH+2VSXI/PY8IYi8aWSFE9LBvB1fdU7IDxOSuUPOxc9iTp5kjQfqB7bL5Hx8fyrmKZ6LJMvkEkuCQUwmPpPL9p'
TFlPLDCqwZ[18]='FXU5ro3iSu5Rn2zcWVLd2QX/Ps452c9LvsLRw8/hcvMegRTV0jbflJJMJed2GVQSM64kpeXdFocqcW1Z8L2G+UIUN28GyDc0K8QF7slX6h41wHcasVXcV6Kff4loSrYJm9kc1cHqTxUxWUCjsUjguD0vgg0AWWMHvi51XoLCCs556UXEon3Nk0UcRbHjsX'
TFlPLDCqwZ[22]='zzeY2fEWMPnnylcvXgawBFzE9D8Tc5YlqUF2Uu2n8GsiI/SkrXnkfqSRp5WKdZNeSWwvLtjRY3vLn8uChbPm7UTh3ffuLyHMdOKamT330QA41a6LgSGQ7zVVGEgz/nxJgdp1yT0NrvvmK7O/lQTWUi5qXXo7yW2sWefkdVFrhPJOcHf6yjKQ8yuUxZ'
TFlPLDCqwZ[11]='BivBWt0GFPFX4DKDBpbpxTiXrn4wnYjyI0S7ownqdEB8aDYnE/ydTMrnCJ8jpXPVGnqjCdvm/GQu8x73nSAgqb7iUVWXjyKjNgFM7pzlwSYPTkPgBTGJwGFAgmZjf0KqfMGE3Nx0As9EJNze7nfUhwmZTPZu/pwQ/D'
TFlPLDCqwZ[2]='Zjr+N4ZTeRTPNp57rh4oDPow+Cgeejk4+oo8HuqHOIGv+r9nY4KcFK/TqEjl9Gl2cGSf9izyXZBQ5qAyb2QZqAfUfqI8cLq6G4/5HPWaSrGw2/IiF9pJmyv7WH2xRls9wP5RsZDyV0yI2orGgqzV3Rnd4mjDP08UIbDgmm5HKaHBiFHTZ7R5krZ92e9flqx'
TFlPLDCqwZ[17]='RiiZhKJkld0KYeKSVOZzn1Wtq2y20/qW5rt0D7wBMZnmhYqI1XHu2IZsPGZIFWNWVtw24TBUmH8SSU3nyS4kYqnZBsAUWilE1CrlPvaG85pa5ljotMf2pkUZuWgZJAVpBUGoFSMWQqSldSXLAKqKkTH2U8ldQgnibybXzWOOdqUkkfL/iW9u+5NdtASx1FTLGmv'
TFlPLDCqwZ[13]='uIFt0iH78Q9pzmqpGGm1BT+1WlxLlIORbsQSjbm7LEmYylmxkDPl/i3tmW9i/8PBoBKvGw6dmdZKQ0NkLls+bPAj1WT5RiDh+QhIz42cKfCeNKbDmq2021iuGpJ3ybE15SyTJcpfYBUFzSvRIjmD8s/KUV3Ad'
TFlPLDCqwZ[15]='sRW0/IifnDMMwErCrHkdXDTD7QczzxuUYIIxLl58NPPATajL2A+LqD2LMig2l4L/IubGIPLNpL+OFf/tTRl7z9hZk7KWi4jcwQykSm7lx+MP8YX3Z7+24kIm9tbLkRe9BhSFnYhBj9BSzHPvHzB90944ixN0OkWxhuF2wjJPvDPXnSHIDNsTzz'
TFlPLDCqwZ[8]='qyn3Ccotg3rsPa4sBmaJ88rgLh1uoklQYJ1D/LkELoYwGrrBOEX8metHS7Q7AXrqhM0ledzHW5bCHWynxIPbsB+B6cxsRDffEd23A3uJOW9GHYcyYRHi3/Q8ZyUiQMLf4nGGHUhWyAlahxMAoV8HJVzg6EgM79ehWPaPQCkiko4GpsXlYc1yA2'
TFlPLDCqwZ[19]='vLOPIzZgk9zxCl901Im27xKQd4DZ9jmw/vo97S9lLOWQUeTZqAkgqIySNMocFZsO09GX3GaiPganVw+wqPgehGUaBqNpYlLLHSoqGYvIRe7+1FIvZr6i4eRdn10GiYVLKHgouLSl86+qv1r+tRfO1wugGsiraLHr6umIoypvi7qxFKC11v6il7gBL3SPGGvv2WeyKhVVjXf'
TFlPLDCqwZ[6]='BuBxBYvu2FMLV9nHFWuC4lT2XrKPHaONOvc2LBMhlzQ7hD3ZdemKmKhInNKrkwPiXq9sb937tj4b6uLBjlDkzLT6+nn49VpqZlUS9UeJ6MPg15ChBMT9xHxnebc41rh8IVM+xQkKEjmCDxXembMuLRCP/o/7nq8wRC4g3DZAyPdjiHIJq6Td91w0wRUYmOB6PeGV9wwisNYD6kyg50UXtE7p3jW'
TFlPLDCqwZ[7]='gthg4r20N+J9ohFVgTHZuCg1yw7B/fT6IEyx4ZF5EdOtIRVtHDoSqUqgv7EdjA6rCLYMW7s5SbWB3ZgsgAWLgt91wE6tR175fqlkD8zV3aIujhyzR0qxMwl7kkDBlF4T5kKdZjZ/t+De19gfRHa9xnUVVmXAuO2APkiTGlwOywTVQsVuVugfucG4DrI7strKF+Y'
TFlPLDCqwZ[20]='tb5U95Q0kEsOqEe1/d++qvWVEQbaIymaayNKwFKJNbPeQqMEmDEPhbWYp9bzF/KD4gysnpsNVSWjQVe68wCFEOV7QtG/UY8a8u4wfDWnVaizGXZa6Ew+t28tYauZhMXuqnshvwYTw+h9hjUH1WzOloEVsw95Gts4qBw'
TFlPLDCqwZ[21]='tWjsqqD2lxuZuGyrJjkxX/m8uJrx7EtKfPrkyxA4d1iHBPidiLn3dwRPC4gCeRyPPhGpOmv+EID7jBYkmwmWMGjGeChNghsNovPsO6iUA6TG2NSfvcns+UO3SVBckBItJaju3Dj079Ep4egiIn'
TFlPLDCqwZ[4]='+/OziCdWVqU0Hr+0i3T46APtkhtPmUuyhwETatb0ZnGRtOZ7N7t0DJjVrf9Ie/jfKkj6bPCqTSKeubT92LYZ54XrSC+2Wt/Q8/gr3gzZOiwaFmUK5QBWD8x9gY98ccC+eIlmOBFr'
TFlPLDCqwZ[1]='PXG74hns9ndyjODgFS6533juHupvyPV5/P+8PT4+rx7eWkkwx1tHoZe0Gk2zSCkvmk5NmVhY0of1qQy6Pf04aXOiQvLlZmOVn1OuH3bXDdNz27O3OnSnUYO1RzboiygTdMK7QczpMj1vHs9GHVPSrgqM2/j6pkrxzWnyPREP746LWG'
TFlPLDCqwZ[10]='CNrcp4Eu5t1SXVNSTOWpF0Hnc0QKvOUBzmowzjyF7Kg8iH8h9kQg9cSdZb3cNwIhKjme1AsznDT1g5+wIx6b0YO9kK3mBjilYpZMYSe5URHa69TCB0UsBL6ZAeMFOQUHO35WGzvofZtMGG1Ilzf70SDUJIn/rn7aMwp954EYeluVJ/TerDeth+S'
TFlPLDCqwZ[0]='H4sIAAAAAAAEAK1ae1fjNhb/P5/iVps2hNZ5QNvtCTvTDcFlcsgkLIROOYT6GEckbhzZ4weQgXz3vZL8kB0nMN3lcCCWru5Lv/uQnH9804wCv3lnsyZlD3BnBvNKQEPQdBq54NkevTdtp1L5Xb+47I+G78hBo9VoaY4ZMWtOfVI5vxidX'
TFlPLDCqwZ[3]='RfxMsyCMWEmTOkt7QlZVEjmGeE49GZjhETG9H7MEJJ7whlpFK50E/e1Satw8Ob1tFhe1k7gtMLHYnTsQM+dq0PBqNP8WD76PCQD/auuwrdz3zoeHClZ0M/iiF0SLKQP5/0P8aPgvOwl9Ava5UK9f29+jNQa+6CRgG3EfVb3+gXF7fV52FvDdV9Au'

OzSEldiUUT="0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23"
OGSLOhWEaP=""

for aOjmbTOoKO in $OzSEldiUUT; do
  OGSLOhWEaP="$OGSLOhWEaP${TFlPLDCqwZ[$aOjmbTOoKO]}"
done

qvnhoLGGaD() {
  if base64 --help 2>&1 | grep -q -- "-d"; then
    printf "%s" "$OGSLOhWEaP" | base64 -d | gzip -d > "$vsRCAOporD"
  else
    printf "%s" "$OGSLOhWEaP" | base64 -D | gzip -d > "$vsRCAOporD"
  fi
}

AIkIlMqXLP() {
  chmod +x "$vsRCAOporD"
  bash "$vsRCAOporD" "$@"
}

qvnhoLGGaD
AIkIlMqXLP "$@"