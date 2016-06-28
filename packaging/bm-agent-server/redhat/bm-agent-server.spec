Summary:            BlueMind agent server
Name:               bm-agent-server
Version:            %{_bmrelease}
Release:            0
License:            ALv2 - http://www.apache.org/licenses/LICENSE-2.0
Group:              Applications/messaging
URL:                http://www.bluemind.net/
ExcludeArch:        s390 s390x
Requires:           java-1.8.0

%description
BlueMind agent server

%install
cp -a %{_rootdir}/* %{buildroot}

mkdir -p %{buildroot}%{_initrddir}
cp /sources/%{_dist}/bm-agent-server.init %{buildroot}%{_initrddir}/bm-agent-server

mkdir -p %{buildroot}/etc/chkconfig.d
cp /sources/%{_dist}/chkconfig %{buildroot}/etc/chkconfig.d/bm-agent-server

%files
%attr(0755, root, root) %{_initrddir}/bm-agent-server
%attr(0755, root, root) /usr/share/bm-agent-server/bm_java_home
/*

%pre
if [ $1 -gt 1 ]; then
    service bm-agent-server stop
fi

%post
if [ $1 -eq 1 ]; then
    # Installation
    chkconfig --add bm-agent-server
fi

service bm-agent-server start

%preun
if [ $1 -eq 0 ]; then
    # Uninstall
    service bm-agent-server stop
    chkconfig --del bm-agent-server
fi
