#!/usr/bin/env perl
use warnings;
use strict;

die "perl $0 <ensembl/gencode.gtf>\n" unless @ARGV eq 1;

my (%gene);
open GTF, $ARGV[0] or die $!;
while(<GTF>)
{
	chomp;
	next if(/^#/);
	my @tmp = split;
	my ($gid) = $_ =~ /gene_id "([^;]+)";/;
	if($_ =~ /gene_biotype "protein_coding";/)
	{
		if($tmp[2] =~ /exon/)
		{
			push @{$gene{$tmp[0]}{$gid}{$tmp[6]}}, "$tmp[3],$tmp[4],1";
		}
	}else{
		if($tmp[2] =~ /exon/)
		{
			push @{$gene{$tmp[0]}{$gid}{$tmp[6]}}, "$tmp[3],$tmp[4],1";
		}
	}
}
close GTF;

open OUT, ">$ARGV[0].myformat" or die $!;
foreach my $c(keys %gene)
{
	foreach my $g(keys %{$gene{$c}})
	{
			foreach my $ps(keys %{$gene{$c}{$g}})
			{
				if($ps eq '+')
				{
					my ($s, $e, $type);
					if(@{$gene{$c}{$g}{$ps}} eq 1)
					{
						($s,$e, $type) = (split /,/, $gene{$c}{$g}{$ps}->[0])[0,1,2];
					}else{
						$s = (split /,/, $gene{$c}{$g}{$ps}->[0])[0];
						$e = (split /,/, $gene{$c}{$g}{$ps}->[-1])[1];
						$type = (split /,/, $gene{$c}{$g}{$ps}->[0])[2];
					}
					my (@start, @end);
					foreach my $loci(@{$gene{$c}{$g}{$ps}})
					{
						push @start, (split /,/, $loci)[0];
						push @end, (split /,/, $loci)[1];
					}
					my $ss = join ",", @start;
					my $ee = join ",", @end;
					print OUT join "\t", $c, $g, "$s-$e", $ps, $type, $ss, $ee, "\n";
				}else{
					my @ps_a = reverse @{$gene{$c}{$g}{$ps}};
					my ($s, $e, $type);
					if(@ps_a eq 1)
					{
						($s,$e,$type) = (split /,/, $ps_a[0])[0,1,2];
					}else{
						$s = (split /,/, $ps_a[0])[0];
						$e = (split /,/, $ps_a[-1])[1];
						$type = (split /,/, $ps_a[0])[2];
					}
					my (@start, @end);
					foreach my $loci(@ps_a)
					{
						push @start, (split /,/, $loci)[0];
						push @end, (split /,/, $loci)[1];
					}
					my $ss = join ",", @start;
					my $ee = join ",", @end;
					print OUT join "\t", $c, $g, "$s-$e", $ps, $type, $ss, $ee, "\n";
				}
			}
		
	}
}
