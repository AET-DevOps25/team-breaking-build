{{/*
Return the name of the chart
*/}}
{{- define "grafana.name" -}}
{{ .Values.name | default .Chart.Name }}
{{- end }}

{{/*
Return the chart name with prefix (customizable fullname)
*/}}
{{- define "grafana.fullname" -}}
{{- if .Values.prefix -}}
{{ .Values.prefix }}-{{ include "grafana.name" . }}
{{- else -}}
{{ include "grafana.name" . }}
{{- end -}}
{{- end }}

{{/*
Return the full service name
*/}}
{{- define "grafana.serviceName" -}}
{{ include "grafana.fullname" . }}-service
{{- end }}

{{/*
Return the full config name
*/}}
{{- define "grafana.configName" -}}
{{ include "grafana.fullname" . }}-config
{{- end }}


{{/*
Return the full service account name
*/}}
{{- define "grafana.serviceAccountName" -}}
{{- include "grafana.fullname" . }}-sa
{{- end }}

{{/*
Return the full role name
*/}}
{{- define "grafana.roleName" -}}
{{- include "grafana.fullname" . }}-role
{{- end }}

{{/*
Return the full role binding name
*/}}
{{- define "grafana.roleBindingName" -}}
{{- include "grafana.fullname" . }}-rolebinding
{{- end }}

{{/*
Return the full pvc name
*/}}
{{- define "grafana.pvcName" -}}
{{- include "grafana.fullname" . }}-pvc
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "grafana.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "grafana.labels" -}}
helm.sh/chart: {{ include "grafana.chart" . }}
{{ include "grafana.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "grafana.selectorLabels" -}}
app.kubernetes.io/name: {{ include "grafana.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
