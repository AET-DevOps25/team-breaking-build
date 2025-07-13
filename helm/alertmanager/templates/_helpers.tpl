{{/*
Return the name of the chart
*/}}
{{- define "prometheus-alertmanager.name" -}}
{{ .Values.name | default .Chart.Name }}
{{- end }}

{{/*
Return the chart name with prefix (customizable fullname)
*/}}
{{- define "prometheus-alertmanager.fullname" -}}
{{- if .Values.prefix -}}
{{ .Values.prefix }}-{{ include "prometheus-alertmanager.name" . }}
{{- else -}}
{{ include "prometheus-alertmanager.name" . }}
{{- end -}}
{{- end }}

{{/*
Return the full service name
*/}}
{{- define "prometheus-alertmanager.serviceName" -}}
{{ include "prometheus-alertmanager.fullname" . }}-service
{{- end }}

{{/*
Return the full config name
*/}}
{{- define "prometheus-alertmanager.configName" -}}
{{ include "prometheus-alertmanager.fullname" . }}-config
{{- end }}


{{/*
Return the full service account name
*/}}
{{- define "prometheus-alertmanager.serviceAccountName" -}}
{{- include "prometheus-alertmanager.fullname" . }}-sa
{{- end }}

{{/*
Return the full role name
*/}}
{{- define "prometheus-alertmanager.roleName" -}}
{{- include "prometheus-alertmanager.fullname" . }}-role
{{- end }}

{{/*
Return the full role binding name
*/}}
{{- define "prometheus-alertmanager.roleBindingName" -}}
{{- include "prometheus-alertmanager.fullname" . }}-rolebinding
{{- end }}

{{/*
Return the full pvc name
*/}}
{{- define "prometheus-alertmanager.pvcName" -}}
{{- include "prometheus-alertmanager.fullname" . }}-pvc
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "prometheus-alertmanager.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "prometheus-alertmanager.labels" -}}
helm.sh/chart: {{ include "prometheus-alertmanager.chart" . }}
{{ include "prometheus-alertmanager.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "prometheus-alertmanager.selectorLabels" -}}
app.kubernetes.io/name: {{ include "prometheus-alertmanager.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
